async function processReturn() {
    const data = {
        returnType: document.getElementById('returnType').value,
        medicineId: parseInt(document.getElementById('medicineId').value),
        quantity: parseInt(document.getElementById('quantity').value),
        reason: document.getElementById('reason').value,
        saleId: document.getElementById('saleId').value ? parseInt(document.getElementById('saleId').value) : null,
        purchaseId: document.getElementById('purchaseId').value ? parseInt(document.getElementById('purchaseId').value) : null
    };
    try {
        const result = await API.post('/api/returns', data);
        showToast('Return processed: ' + result.returnNumber);
    } catch (e) { showToast(e.message, 'danger'); }
}
