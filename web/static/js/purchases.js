let purchaseItems = [];

function addPurchaseItem() {
    const medicineId = document.getElementById('itemMedicineId').value;
    const quantity = parseInt(document.getElementById('itemQuantity').value);
    const price = parseFloat(document.getElementById('itemPrice').value);
    const expiry = document.getElementById('itemExpiry').value;
    const batch = document.getElementById('itemBatch').value;
    const medicineName = document.getElementById('itemMedicineId').selectedOptions[0].text;

    if (!medicineId || !quantity || !price) {
        showToast('Fill medicine, quantity and price', 'warning');
        return;
    }

    purchaseItems.push({ medicineId: parseInt(medicineId), medicineName, quantity, purchasePrice: price, expiryDate: expiry || null, batchNumber: batch });
    renderPurchaseItems();
}

function renderPurchaseItems() {
    document.getElementById('purchaseItemsBody').innerHTML = purchaseItems.map((item, i) => `
        <tr>
            <td>${item.medicineName}</td>
            <td>${item.quantity}</td>
            <td>₹${item.purchasePrice}</td>
            <td>${item.expiryDate || '-'}</td>
            <td>${item.batchNumber || '-'}</td>
            <td><button class="btn btn-sm btn-outline-danger" onclick="purchaseItems.splice(${i},1);renderPurchaseItems()"><i class="bi bi-trash"></i></button></td>
        </tr>
    `).join('');
}

async function savePurchase() {
    const supplierId = document.getElementById('supplierId').value;
    const purchaseDate = document.getElementById('purchaseDate').value;
    if (!supplierId || !purchaseDate || purchaseItems.length === 0) {
        showToast('Fill all required fields and add items', 'warning');
        return;
    }
    const data = {
        invoiceNumber: document.getElementById('invoiceNumber').value || null,
        supplierId: parseInt(supplierId),
        purchaseDate: purchaseDate,
        items: purchaseItems.map(i => ({
            medicineId: i.medicineId,
            quantity: i.quantity,
            purchasePrice: i.purchasePrice,
            expiryDate: i.expiryDate,
            batchNumber: i.batchNumber
        }))
    };
    try {
        const result = await API.post('/api/purchases', data);
        showToast('Purchase saved: ' + result.invoiceNumber);
        purchaseItems = [];
        renderPurchaseItems();
    } catch (e) { showToast(e.message, 'danger'); }
}
