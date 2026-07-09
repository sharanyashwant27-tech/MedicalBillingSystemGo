function openSupplierModal() {
    document.getElementById('supplierId').value = '';
    document.getElementById('supplierName').value = '';
    document.getElementById('gstNumber').value = '';
    document.getElementById('contactPerson').value = '';
    document.getElementById('phone').value = '';
    document.getElementById('email').value = '';
    document.getElementById('state').value = '';
    document.getElementById('address').value = '';
    document.getElementById('pinCode').value = '';
}

async function editSupplier(id) {
    try {
        const s = await API.get('/api/suppliers/' + id);
        document.getElementById('supplierId').value = s.id;
        document.getElementById('supplierName').value = s.supplierName;
        document.getElementById('gstNumber').value = s.gstNumber || '';
        document.getElementById('contactPerson').value = s.contactPerson || '';
        document.getElementById('phone').value = s.phone || '';
        document.getElementById('email').value = s.email || '';
        document.getElementById('state').value = s.state || '';
        document.getElementById('address').value = s.address || '';
        document.getElementById('pinCode').value = s.pinCode || '';
        new bootstrap.Modal(document.getElementById('supplierModal')).show();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function saveSupplier() {
    const id = document.getElementById('supplierId').value;
    const data = {
        supplierName: document.getElementById('supplierName').value,
        gstNumber: document.getElementById('gstNumber').value,
        contactPerson: document.getElementById('contactPerson').value,
        phone: document.getElementById('phone').value,
        email: document.getElementById('email').value,
        state: document.getElementById('state').value,
        address: document.getElementById('address').value,
        pinCode: document.getElementById('pinCode').value
    };
    try {
        if (id) await API.put('/api/suppliers/' + id, data);
        else await API.post('/api/suppliers', data);
        showToast('Supplier saved');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function deleteSupplier(id) {
    if (!confirm('Delete this supplier?')) return;
    try {
        await API.delete('/api/suppliers/' + id);
        showToast('Supplier deleted');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}
