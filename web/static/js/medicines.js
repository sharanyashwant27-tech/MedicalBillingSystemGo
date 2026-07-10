function openMedicineModal() {
    document.getElementById('medicineForm').reset();
    document.getElementById('medicineId').value = '';
}

async function editMedicine(id) {
    try {
        const m = await API.get('/api/medicines/' + id);
        document.getElementById('medicineId').value = m.id;
        document.getElementById('medicineCode').value = m.medicineCode || '';
        document.getElementById('medicineName').value = m.medicineName || '';
        document.getElementById('categoryId').value = m.categoryId || '';
        document.getElementById('brand').value = m.brand || '';
        document.getElementById('barcode').value = m.barcode || '';
        document.getElementById('batchNumber').value = m.batchNumber || '';
        document.getElementById('expiryDate').value = m.expiryDate || '';
        document.getElementById('manufacturingDate').value = m.manufacturingDate || '';
        document.getElementById('hsnCode').value = m.hsnCode || '';
        document.getElementById('gstPercent').value = m.gstPercent || '';
        document.getElementById('purchasePrice').value = m.purchasePrice || '';
        document.getElementById('sellingPrice').value = m.sellingPrice || '';
        document.getElementById('mrp').value = m.mrp || '';
        document.getElementById('discountPercent').value = m.discountPercent || '';
        document.getElementById('minimumStock').value = m.minimumStock || '';
        document.getElementById('currentStock').value = m.currentStock || '';
        document.getElementById('rackNumber').value = m.rackNumber || '';
        document.getElementById('supplierId').value = m.supplierId || '';
        new bootstrap.Modal(document.getElementById('medicineModal')).show();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function saveMedicine() {
    const id = document.getElementById('medicineId').value;
    const data = {
        medicineCode: document.getElementById('medicineCode').value || 'MED-AUTO',
        medicineName: document.getElementById('medicineName').value,
        categoryId: document.getElementById('categoryId').value ? parseInt(document.getElementById('categoryId').value) : null,
        brand: document.getElementById('brand').value,
        barcode: document.getElementById('barcode').value,
        batchNumber: document.getElementById('batchNumber').value,
        expiryDate: document.getElementById('expiryDate').value || null,
        manufacturingDate: document.getElementById('manufacturingDate').value || null,
        hsnCode: document.getElementById('hsnCode').value,
        gstPercent: parseFloat(document.getElementById('gstPercent').value) || 0,
        purchasePrice: parseFloat(document.getElementById('purchasePrice').value),
        sellingPrice: parseFloat(document.getElementById('sellingPrice').value),
        mrp: parseFloat(document.getElementById('mrp').value) || null,
        discountPercent: parseFloat(document.getElementById('discountPercent').value) || 0,
        minimumStock: parseInt(document.getElementById('minimumStock').value) || 10,
        currentStock: parseInt(document.getElementById('currentStock').value) || 0,
        rackNumber: document.getElementById('rackNumber').value,
        supplierId: document.getElementById('supplierId').value ? parseInt(document.getElementById('supplierId').value) : null
    };
    try {
        if (id) await API.put('/api/medicines/' + id, data);
        else await API.post('/api/medicines', data);
        showToast('Medicine saved');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function deleteMedicine(id) {
    if (!confirm('Deactivate this medicine?')) return;
    try {
        await API.delete('/api/medicines/' + id);
        showToast('Medicine deactivated');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}
