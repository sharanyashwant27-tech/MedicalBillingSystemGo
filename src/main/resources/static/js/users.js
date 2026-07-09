function openUserModal() {
    document.getElementById('userId').value = '';
    document.getElementById('username').value = '';
    document.getElementById('password').value = '';
    document.getElementById('fullName').value = '';
    document.getElementById('email').value = '';
    document.getElementById('role').value = 'ROLE_CASHIER';
}

async function saveUser() {
    const data = {
        username: document.getElementById('username').value,
        password: document.getElementById('password').value,
        fullName: document.getElementById('fullName').value,
        email: document.getElementById('email').value,
        roles: [document.getElementById('role').value]
    };
    try {
        await API.post('/api/users', data);
        showToast('User created');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function lockUser(id) {
    try { await API.post('/api/users/' + id + '/lock', {}); showToast('User locked'); location.reload(); }
    catch (e) { showToast(e.message, 'danger'); }
}

async function unlockUser(id) {
    try { await API.post('/api/users/' + id + '/unlock', {}); showToast('User unlocked'); location.reload(); }
    catch (e) { showToast(e.message, 'danger'); }
}

async function resetPassword(id) {
    const password = prompt('Enter new password:');
    if (!password) return;
    try { await API.post('/api/users/' + id + '/reset-password', { password }); showToast('Password reset'); }
    catch (e) { showToast(e.message, 'danger'); }
}

async function deleteUser(id) {
    if (!confirm('Delete this user?')) return;
    try { await API.delete('/api/users/' + id); showToast('User deleted'); location.reload(); }
    catch (e) { showToast(e.message, 'danger'); }
}
