function openCustomerModal() {
    document.getElementById('customerId').value = '';
    ['customerName','phone','email','age','doctorName','gstNumber','address'].forEach(id => {
        document.getElementById(id).value = '';
    });
    document.getElementById('gender').value = '';
}

async function editCustomer(id) {
    try {
        const c = await API.get('/api/customers').then(list => list.find(x => x.id === id));
        if (!c) return;
        document.getElementById('customerId').value = c.id;
        document.getElementById('customerName').value = c.customerName;
        document.getElementById('phone').value = c.phone || '';
        document.getElementById('email').value = c.email || '';
        document.getElementById('age').value = c.age || '';
        document.getElementById('gender').value = c.gender || '';
        document.getElementById('doctorName').value = c.doctorName || '';
        document.getElementById('gstNumber').value = c.gstNumber || '';
        document.getElementById('address').value = c.address || '';
        new bootstrap.Modal(document.getElementById('customerModal')).show();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function saveCustomer() {
    const id = document.getElementById('customerId').value;
    const data = {
        customerName: document.getElementById('customerName').value,
        phone: document.getElementById('phone').value,
        email: document.getElementById('email').value,
        age: document.getElementById('age').value ? parseInt(document.getElementById('age').value) : null,
        gender: document.getElementById('gender').value || null,
        doctorName: document.getElementById('doctorName').value,
        gstNumber: document.getElementById('gstNumber').value,
        address: document.getElementById('address').value
    };
    try {
        if (id) await API.put('/api/customers/' + id, data);
        else await API.post('/api/customers', data);
        showToast('Customer saved');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function deleteCustomer(id) {
    if (!confirm('Delete this customer?')) return;
    try {
        await API.delete('/api/customers/' + id);
        showToast('Customer deleted');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function viewHistory(id) {
    try {
        const history = await API.get('/api/customers/' + id + '/history');
        alert('Total Purchases: ' + history.totalPurchases);
    } catch (e) { showToast(e.message, 'danger'); }
}
