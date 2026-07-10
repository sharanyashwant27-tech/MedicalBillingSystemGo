let purchaseItems = [];

document.addEventListener('DOMContentLoaded', () => {
    loadPurchases();
    const card = document.getElementById('purchaseFormCard');
    if (card) card.style.display = 'none';
});

function formatMoney(amount) {
    return '₹' + parseFloat(amount || 0).toFixed(2);
}

function formatDate(value) {
    if (!value) return '-';
    return String(value).substring(0, 10);
}

function showPurchaseMessage(message, type = 'success') {
    const alert = document.getElementById('purchaseAlert');
    if (alert) {
        alert.className = `alert alert-${type}`;
        alert.textContent = message;
        alert.classList.remove('d-none');
        alert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        setTimeout(() => alert.classList.add('d-none'), 5000);
    }
    if (typeof showToast === 'function') {
        showToast(message, type);
    }
}

function buildPurchaseItemPayload(item) {
    const payload = {
        medicineId: item.medicineId,
        quantity: item.quantity,
        purchasePrice: item.purchasePrice,
        batchNumber: item.batchNumber || ''
    };
    if (item.expiryDate) {
        payload.expiryDate = item.expiryDate;
    }
    return payload;
}

async function loadPurchases() {
    const tbody = document.getElementById('purchasesBody');
    if (!tbody) return;
    try {
        const purchases = await API.get('/api/purchases');
        if (!purchases.length) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-muted text-center">No purchases yet</td></tr>';
            return;
        }
        tbody.innerHTML = purchases.map(p => `
            <tr>
                <td>${p.invoiceNumber}</td>
                <td>${p.supplier ? p.supplier.supplierName : '-'}</td>
                <td>${formatDate(p.purchaseDate)}</td>
                <td>${(p.items || []).length}</td>
                <td>${formatMoney(p.grandTotal)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-info" onclick="viewPurchase(${p.id})" title="View"><i class="bi bi-eye"></i></button>
                    <button class="btn btn-sm btn-outline-primary" onclick="editPurchase(${p.id})" title="Edit"><i class="bi bi-pencil"></i></button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deletePurchase(${p.id})" title="Delete"><i class="bi bi-trash"></i></button>
                </td>
            </tr>
        `).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-danger text-center">${e.message}</td></tr>`;
    }
}

function openNewPurchase() {
    document.getElementById('purchaseId').value = '';
    document.getElementById('invoiceNumber').value = '';
    document.getElementById('supplierId').value = '';
    document.getElementById('purchaseDate').value = new Date().toISOString().split('T')[0];
    clearItemInputs();
    purchaseItems = [];
    renderPurchaseItems();
    document.getElementById('purchaseFormTitle').innerHTML = '<i class="bi bi-bag-plus"></i> New Purchase';
    document.getElementById('purchaseFormCard').style.display = 'block';
    document.getElementById('purchaseFormCard').scrollIntoView({ behavior: 'smooth' });
}

function cancelPurchaseForm() {
    document.getElementById('purchaseFormCard').style.display = 'none';
    document.getElementById('purchaseId').value = '';
    purchaseItems = [];
    renderPurchaseItems();
}

function clearItemInputs() {
    document.getElementById('itemMedicineId').value = '';
    document.getElementById('itemQuantity').value = '';
    document.getElementById('itemPrice').value = '';
    document.getElementById('itemExpiry').value = '';
    document.getElementById('itemBatch').value = '';
}

function addPurchaseItem() {
    const medicineSelect = document.getElementById('itemMedicineId');
    const medicineId = medicineSelect.value;
    const quantity = parseInt(document.getElementById('itemQuantity').value, 10);
    const price = parseFloat(document.getElementById('itemPrice').value);
    const expiry = document.getElementById('itemExpiry').value;
    const batch = document.getElementById('itemBatch').value;
    const medicineName = medicineSelect.selectedOptions[0]?.text || '';

    if (!medicineId || !quantity || quantity < 1 || !price || price <= 0) {
        showPurchaseMessage('Fill medicine, quantity and price', 'warning');
        return;
    }

    purchaseItems.push({
        medicineId: parseInt(medicineId, 10),
        medicineName,
        quantity,
        purchasePrice: price,
        expiryDate: expiry || null,
        batchNumber: batch || ''
    });
    clearItemInputs();
    renderPurchaseItems();
}

function removePurchaseItem(index) {
    purchaseItems.splice(index, 1);
    renderPurchaseItems();
}

function renderPurchaseItems() {
    const tbody = document.getElementById('purchaseItemsBody');
    const totalEl = document.getElementById('purchaseGrandTotal');
    if (!purchaseItems.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-muted">No items added</td></tr>';
        totalEl.textContent = formatMoney(0);
        return;
    }

    let grandTotal = 0;
    tbody.innerHTML = purchaseItems.map((item, i) => {
        const lineTotal = item.quantity * item.purchasePrice;
        grandTotal += lineTotal;
        return `
            <tr>
                <td>${item.medicineName}</td>
                <td>${item.quantity}</td>
                <td>${formatMoney(item.purchasePrice)}</td>
                <td>${item.expiryDate || '-'}</td>
                <td>${item.batchNumber || '-'}</td>
                <td><button type="button" class="btn btn-sm btn-outline-danger" onclick="removePurchaseItem(${i})"><i class="bi bi-trash"></i></button></td>
            </tr>
        `;
    }).join('');
    totalEl.textContent = formatMoney(grandTotal);
}

async function savePurchase() {
    const purchaseId = document.getElementById('purchaseId').value;
    const supplierId = document.getElementById('supplierId').value;
    const purchaseDate = document.getElementById('purchaseDate').value;
    if (!supplierId || !purchaseDate || purchaseItems.length === 0) {
        showPurchaseMessage('Fill supplier, date, and at least one item', 'warning');
        return;
    }

    const data = {
        invoiceNumber: document.getElementById('invoiceNumber').value.trim(),
        supplierId: parseInt(supplierId, 10),
        purchaseDate,
        items: purchaseItems.map(buildPurchaseItemPayload)
    };

    const saveBtn = document.querySelector('#purchaseFormCard .btn-success');
    if (saveBtn) saveBtn.disabled = true;

    try {
        let result;
        if (purchaseId) {
            result = await API.put('/api/purchases/' + purchaseId, data);
            showPurchaseMessage('Purchase updated successfully. Invoice: ' + result.invoiceNumber, 'success');
        } else {
            result = await API.post('/api/purchases', data);
            showPurchaseMessage('Purchase saved successfully. Invoice: ' + result.invoiceNumber, 'success');
        }
        cancelPurchaseForm();
        await loadPurchases();
    } catch (e) {
        showPurchaseMessage(e.message || 'Failed to save purchase', 'danger');
    } finally {
        if (saveBtn) saveBtn.disabled = false;
    }
}

async function viewPurchase(id) {
    try {
        const p = await API.get('/api/purchases/' + id);
        const itemsHtml = (p.items || []).map(item => `
            <tr>
                <td>${item.medicine ? item.medicine.medicineName : item.medicineId}</td>
                <td>${item.quantity}</td>
                <td>${formatMoney(item.purchasePrice)}</td>
                <td>${formatDate(item.expiryDate)}</td>
                <td>${item.batchNumber || '-'}</td>
                <td>${formatMoney(item.subtotal)}</td>
            </tr>
        `).join('');

        document.getElementById('viewPurchaseBody').innerHTML = `
            <div class="row mb-3">
                <div class="col-md-4"><strong>Invoice:</strong> ${p.invoiceNumber}</div>
                <div class="col-md-4"><strong>Supplier:</strong> ${p.supplier ? p.supplier.supplierName : '-'}</div>
                <div class="col-md-4"><strong>Date:</strong> ${formatDate(p.purchaseDate)}</div>
            </div>
            <div class="table-responsive">
                <table class="table table-sm">
                    <thead><tr><th>Medicine</th><th>Qty</th><th>Price</th><th>Expiry</th><th>Batch</th><th>Subtotal</th></tr></thead>
                    <tbody>${itemsHtml}</tbody>
                    <tfoot>
                        <tr><th colspan="5" class="text-end">Grand Total</th><th>${formatMoney(p.grandTotal)}</th></tr>
                    </tfoot>
                </table>
            </div>
        `;
        new bootstrap.Modal(document.getElementById('viewPurchaseModal')).show();
    } catch (e) {
        showPurchaseMessage(e.message || 'Failed to load purchase', 'danger');
    }
}

async function editPurchase(id) {
    try {
        const p = await API.get('/api/purchases/' + id);
        document.getElementById('purchaseId').value = p.id;
        document.getElementById('invoiceNumber').value = p.invoiceNumber || '';
        document.getElementById('supplierId').value = String(p.supplierId);
        document.getElementById('purchaseDate').value = formatDate(p.purchaseDate);
        purchaseItems = (p.items || []).map(item => ({
            medicineId: item.medicineId,
            medicineName: item.medicine ? item.medicine.medicineName : String(item.medicineId),
            quantity: item.quantity,
            purchasePrice: item.purchasePrice,
            expiryDate: formatDate(item.expiryDate) !== '-' ? formatDate(item.expiryDate) : null,
            batchNumber: item.batchNumber || ''
        }));
        renderPurchaseItems();
        document.getElementById('purchaseFormTitle').innerHTML = '<i class="bi bi-pencil"></i> Edit Purchase';
        document.getElementById('purchaseFormCard').style.display = 'block';
        document.getElementById('purchaseFormCard').scrollIntoView({ behavior: 'smooth' });
    } catch (e) {
        showPurchaseMessage(e.message || 'Failed to load purchase for editing', 'danger');
    }
}

async function deletePurchase(id) {
    if (!confirm('Delete this purchase? Stock will be reversed.')) return;
    try {
        await API.delete('/api/purchases/' + id);
        showPurchaseMessage('Purchase deleted successfully', 'success');
        await loadPurchases();
    } catch (e) {
        showPurchaseMessage(e.message || 'Failed to delete purchase', 'danger');
    }
}
