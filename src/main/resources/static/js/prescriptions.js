async function uploadPrescription() {
    const customerId = document.getElementById('customerId').value;
    const file = document.getElementById('prescriptionFile').files[0];
    const notes = document.getElementById('notes').value;
    if (!customerId || !file) { showToast('Select customer and file', 'warning'); return; }

    const formData = new FormData();
    formData.append('customerId', customerId);
    formData.append('file', file);
    if (notes) formData.append('notes', notes);

    try {
        await API.post('/api/prescriptions', formData);
        showToast('Prescription uploaded');
    } catch (e) { showToast(e.message, 'danger'); }
}
