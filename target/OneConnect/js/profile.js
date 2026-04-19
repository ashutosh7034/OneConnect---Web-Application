function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE_PROFILE = getApiBase();

document.addEventListener('DOMContentLoaded', async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    loadProfile(userId);

    document.getElementById('profileForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const alert = document.getElementById('profileAlert');
        const name = document.getElementById('name').value;
        const email = document.getElementById('email').value;
        
        try {
            const res = await fetch(`${API_BASE_PROFILE}/profile`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, name, email })
            });
            const data = await res.json();
            
            if (data.success) {
                showAlert(alert, data.message, 'success');
                loadProfile(userId); // refresh display
            } else {
                showAlert(alert, data.message, 'error');
            }
        } catch (err) {
            showAlert(alert, 'Network error', 'error');
        }
    });

    document.getElementById('passwordForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const alert = document.getElementById('passwordAlert');
        const oldPassword = document.getElementById('oldPassword').value;
        const newPassword = document.getElementById('newPassword').value;
        const confirmPassword = document.getElementById('confirmPassword').value;
        
        if (newPassword !== confirmPassword) {
            showAlert(alert, 'New passwords do not match', 'error');
            return;
        }

        try {
            const res = await fetch(`${API_BASE_PROFILE}/profile/password`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ userId, oldPassword, newPassword })
            });
            const data = await res.json();
            
            if (data.success) {
                showAlert(alert, data.message, 'success');
                document.getElementById('passwordForm').reset();
            } else {
                showAlert(alert, data.message, 'error');
            }
        } catch (err) {
            showAlert(alert, 'Network error', 'error');
        }
    });
});

async function loadProfile(userId) {
    try {
        const response = await fetch(`${API_BASE_PROFILE}/profile?userId=${userId}`);
        const data = await response.json();
        
        if (data.success) {
            // Update Sidebar
            document.getElementById('displayName').textContent = data.name;
            document.getElementById('displayEmail').textContent = data.email;
            document.getElementById('displayUsername').textContent = `@${data.username}`;
            
            // Generate initials
            const initials = data.name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
            document.getElementById('avatarInitials').textContent = initials;
            
            // Populate form
            document.getElementById('name').value = data.name;
            document.getElementById('email').value = data.email;
            
            // Update local storage username if it changed
            localStorage.setItem('username', data.username);
        }
    } catch (error) {
        console.error("Error loading profile:", error);
    }
}
