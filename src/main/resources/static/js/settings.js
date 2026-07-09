async function saveSettings() {
    const data = {
        shopName: document.getElementById('shopName').value,
        gstNumber: document.getElementById('gstNumber').value,
        phone: document.getElementById('phone').value,
        email: document.getElementById('email').value,
        address: document.getElementById('address').value,
        invoiceFooter: document.getElementById('invoiceFooter').value,
        defaultGstPercent: parseFloat(document.getElementById('defaultGstPercent').value)
    };
    try {
        await API.put('/api/settings', data);
        showToast('Settings saved');
    } catch (e) { showToast(e.message, 'danger'); }
}

async function uploadLogo() {
    const file = document.getElementById('logoFile').files[0];
    if (!file) { showToast('Select a logo file', 'warning'); return; }
    const formData = new FormData();
    formData.append('file', file);
    try {
        await API.post('/api/settings/logo', formData);
        showToast('Logo uploaded');
    } catch (e) { showToast(e.message, 'danger'); }
}

async function backupDatabase() {
    try {
        const result = await API.post('/api/settings/backup', {});
        showToast('Backup created: ' + result.path);
    } catch (e) { showToast(e.message, 'danger'); }
}
