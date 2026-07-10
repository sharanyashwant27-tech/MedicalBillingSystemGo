function editCategory(id, name, description) {
    document.getElementById('categoryId').value = id;
    document.getElementById('categoryName').value = name || '';
    document.getElementById('categoryDesc').value = description || '';
    new bootstrap.Modal(document.getElementById('categoryModal')).show();
}

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.edit-category-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            editCategory(btn.dataset.id, btn.dataset.name, btn.dataset.description);
        });
    });
    document.querySelectorAll('.delete-category-btn').forEach(btn => {
        btn.addEventListener('click', () => deleteCategory(btn.dataset.id));
    });
});

async function saveCategory() {
    const id = document.getElementById('categoryId').value;
    const data = { name: document.getElementById('categoryName').value, description: document.getElementById('categoryDesc').value };
    try {
        if (id) await API.put('/api/categories/' + id, data);
        else await API.post('/api/categories', data);
        showToast('Category saved');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}

async function deleteCategory(id) {
    if (!confirm('Delete this category?')) return;
    try {
        await API.delete('/api/categories/' + id);
        showToast('Category deleted');
        location.reload();
    } catch (e) { showToast(e.message, 'danger'); }
}
