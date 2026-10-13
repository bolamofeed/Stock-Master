const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

/**
 * 1. RBAC Custom Claims Trigger
 * When a user document is created or updated in Firestore, sync their role
 * into Firebase Auth custom user claims.
 */
exports.syncUserRoleCustomClaims = functions.firestore
  .document("users/{userId}")
  .onWrite(async (change, context) => {
    const userId = context.params.userId;
    if (!change.after.exists) {
      return null;
    }

    const userData = change.after.data();
    const role = userData.role || "auditor";
    const supplierName = userData.assignedVendorName || "";

    try {
      await admin.auth().setCustomUserClaims(userId, {
        role: role,
        supplierName: supplierName,
      });
      console.log(`Custom claims updated for ${userId} with role ${role}`);
    } catch (error) {
      console.error(`Failed to assign custom claims for ${userId}:`, error);
    }
  });

/**
 * 2. Automated Low-Stock Alert & FCM Push Notification Trigger
 * Monitors inventory item updates and triggers automated push notifications
 * when an item reaches or drops below minimum stock level.
 */
exports.onItemStockUpdated = functions.firestore
  .document("items/{itemId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Check if quantity decreased to or below minimum threshold
    if (after.quantity <= after.minStock && before.quantity > after.minStock) {
      const isOutOfStock = after.quantity <= 0;
      const title = isOutOfStock
        ? `🚨 Out of Stock: ${after.name}`
        : `⚠️ Low Stock Warning: ${after.name}`;
      const body = `${after.name} (${after.sku}) is at ${after.quantity} ${after.unit}. Min limit is ${after.minStock}.`;

      // 1. Write to in-app notifications collection
      await db.collection("notifications").add({
        title: title,
        body: body,
        severity: isOutOfStock ? "DANGER" : "WARNING",
        itemId: context.params.itemId,
        isRead: false,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });

      // 2. Broadcast FCM Push Notification to Managers and Admins topic
      const payload = {
        notification: {
          title: title,
          body: body,
        },
        data: {
          itemId: context.params.itemId,
          type: "LOW_STOCK_ALERT",
        },
        topic: "managers_and_admins",
      };

      try {
        await admin.messaging().send(payload);
        console.log(`FCM low stock push alert dispatched for ${after.sku}`);
      } catch (err) {
        console.warn("FCM push send failed:", err);
      }
    }
  });

/**
 * 3. Transactional Invoice Confirmation and Stock Movement
 * Atomically updates stock items inside a Firestore Transaction
 * preventing negative stock and ensuring atomic consistency.
 */
exports.confirmInvoiceStockUpdate = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be authenticated.");
  }

  const { invoiceId, items, invoiceType } = data;
  if (!items || !Array.isArray(items) || items.length === 0) {
    throw new functions.https.HttpsError("invalid-argument", "Invoice items cannot be empty.");
  }

  return db.runTransaction(async (transaction) => {
    // 1. Read all item documents
    const itemRefs = items.map((it) => db.collection("items").doc(it.itemId));
    const itemSnapshots = await Promise.all(itemRefs.map((ref) => transaction.get(ref)));

    // 2. Validate stock sufficiency
    for (let i = 0; i < items.length; i++) {
      const snap = itemSnapshots[i];
      if (!snap.exists) {
        throw new functions.https.HttpsError("not-found", `Item ${items[i].itemId} not found.`);
      }
      const currentQty = snap.data().quantity;
      const requestedQty = items[i].quantity;

      if (invoiceType === "SALES" && currentQty < requestedQty) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          `Insufficient stock for ${snap.data().name}. Available: ${currentQty}, Requested: ${requestedQty}`
        );
      }
    }

    // 3. Atomically update inventory and write stock movements
    for (let i = 0; i < items.length; i++) {
      const snap = itemSnapshots[i];
      const itemData = snap.data();
      const delta = invoiceType === "SALES" ? -items[i].quantity : items[i].quantity;
      const newQty = itemData.quantity + delta;

      transaction.update(itemRefs[i], {
        quantity: newQty,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // Record immutable stock movement
      const movementRef = db.collection("stockMovements").doc();
      transaction.set(movementRef, {
        itemId: items[i].itemId,
        itemName: itemData.name,
        type: invoiceType === "SALES" ? "STOCK_OUT" : "STOCK_IN",
        quantityChange: delta,
        previousQuantity: itemData.quantity,
        newQuantity: newQty,
        userId: context.auth.uid,
        invoiceId: invoiceId,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // 4. Mark invoice as confirmed / paid
    const invoiceRef = db.collection("invoices").doc(invoiceId);
    transaction.update(invoiceRef, {
      status: "PAID",
      confirmedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // 5. If saveCustomer is active and it's a sales invoice, transactionally create/update customer record
    const { saveCustomer, customerName, customerPhone, totalAmount } = data;
    if (saveCustomer && invoiceType === "SALES" && (customerName || customerPhone)) {
      const cleanPhone = (customerPhone || "").replace(/[\s\-\(\)]/g, "");
      const normPhone = cleanPhone.startsWith("0") ? "+20" + cleanPhone.slice(1) : cleanPhone;
      const customersColl = db.collection("customers");

      let customerRef = null;
      let existingCustomerData = null;

      if (normPhone) {
        const phoneQuery = await transaction.get(customersColl.where("p", "==", normPhone).limit(1));
        if (!phoneQuery.empty) {
          customerRef = phoneQuery.docs[0].ref;
          existingCustomerData = phoneQuery.docs[0].data();
        }
      }

      if (!customerRef && customerName) {
        const nameQuery = await transaction.get(customersColl.where("nl", "==", customerName.trim().toLowerCase()).limit(1));
        if (!nameQuery.empty) {
          customerRef = nameQuery.docs[0].ref;
          existingCustomerData = nameQuery.docs[0].data();
        }
      }

      const invoiceMinorTotal = totalAmount ? Math.round(Number(totalAmount) * 100) : 0;
      const nowYmd = parseInt(new Date().toISOString().slice(0, 10).replace(/-/g, ""), 10);

      if (customerRef && existingCustomerData) {
        transaction.update(customerRef, {
          n: customerName ? customerName.trim() : existingCustomerData.n,
          nl: customerName ? customerName.trim().toLowerCase() : existingCustomerData.nl,
          p: normPhone || existingCustomerData.p,
          tp: admin.firestore.FieldValue.increment(invoiceMinorTotal),
          ic: admin.firestore.FieldValue.increment(1),
          lpd: nowYmd,
          ua: admin.firestore.FieldValue.serverTimestamp(),
        });
      } else {
        const newCustRef = customersColl.doc();
        transaction.set(newCustRef, {
          n: customerName ? customerName.trim() : "Valued Customer",
          nl: (customerName || "valued customer").trim().toLowerCase(),
          p: normPhone || "",
          em: "",
          ad: "",
          no: "",
          mc: false,
          st: 1,
          tp: invoiceMinorTotal,
          ic: 1,
          lpd: nowYmd,
          lra: 0,
          ca: admin.firestore.FieldValue.serverTimestamp(),
          ua: admin.firestore.FieldValue.serverTimestamp(),
        });
      }
    }

    return { success: true, message: "Invoice confirmed, stock updated, and customer processed transactionally." };
  });
});

