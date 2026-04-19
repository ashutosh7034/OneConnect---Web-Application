function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE = getApiBase();

document.addEventListener('DOMContentLoaded', () => {
    
    // Handle Login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('loginBtn');
            const alert = document.getElementById('loginAlert');
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;

            btn.disabled = true;
            btn.innerHTML = 'Logging in...';
            alert.style.display = 'none';

            try {
                const response = await fetch(`${API_BASE}/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ email, password })
                });

                const raw = await response.text();
                let data = {};

                try {
                    data = raw ? JSON.parse(raw) : {};
                } catch (parseError) {
                    data = { success: false, message: raw || 'Unexpected response from server' };
                }

                if (!response.ok && !data.message) {
                    data.message = `Login request failed (${response.status})`;
                }

                if (data.success) {
                    localStorage.setItem('userId', data.userId);
                    localStorage.setItem('username', data.username);
                    window.location.href = 'dashboard.html';
                } else {
                    showAlert(alert, data.message || 'Login failed', 'error');
                }
            } catch (error) {
                showAlert(alert, 'Network error. Please try again.', 'error');
            } finally {
                btn.disabled = false;
                btn.innerHTML = 'Login';
            }
        });
    }

    // Handle Registration
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const btn = document.getElementById('registerBtn');
            const alert = document.getElementById('registerAlert');
            
            const name = document.getElementById('name').value;
            const email = document.getElementById('email').value;
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;

            btn.disabled = true;
            btn.innerHTML = 'Registering...';
            alert.style.display = 'none';

            try {
                const response = await fetch(`${API_BASE}/register`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, username, password })
                });

                const raw = await response.text();
                let data = {};

                try {
                    data = raw ? JSON.parse(raw) : {};
                } catch (parseError) {
                    data = { success: false, message: raw || 'Unexpected response from server' };
                }

                if (!response.ok && !data.message) {
                    data.message = `Registration request failed (${response.status})`;
                }

                if (data.success) {
                    showAlert(alert, 'Registration successful! Redirecting to login...', 'success');
                    setTimeout(() => {
                        window.location.href = 'index.html';
                    }, 2000);
                } else {
                    showAlert(alert, data.message || 'Registration failed', 'error');
                    btn.disabled = false;
                    btn.innerHTML = 'Register';
                }
            } catch (error) {
                showAlert(alert, 'Network error. Please try again.', 'error');
                btn.disabled = false;
                btn.innerHTML = 'Register';
            }
        });
    }
});

function showAlert(element, message, type) {
    element.textContent = message;
    element.className = `alert alert-${type}`;
    element.style.display = 'block';
}

function logout() {
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    window.location.href = 'index.html';
}
