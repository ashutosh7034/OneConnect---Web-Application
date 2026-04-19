function getApiBase() {
    const pathParts = window.location.pathname.split('/').filter(Boolean);
    const contextPath = pathParts.length > 1 ? `/${pathParts[0]}` : '';
    return `${window.location.origin}${contextPath}/api`;
}

const API_BASE_FAV = getApiBase();

function normalizeUrl(rawUrl) {
    if (!rawUrl) return '';
    return rawUrl.startsWith('http') ? rawUrl : `https://${rawUrl}`;
}

function getLogoUrl(serviceUrl) {
    try {
        const host = new URL(normalizeUrl(serviceUrl)).hostname;
        return `https://www.google.com/s2/favicons?domain=${encodeURIComponent(host)}&sz=128`;
    } catch (error) {
        return '';
    }
}

function buildServiceIcon(serviceName, serviceUrl) {
    const logoUrl = getLogoUrl(serviceUrl);
    const fallback = ((serviceName || '?').trim().charAt(0) || '?').toUpperCase();

    if (!logoUrl) {
        return `<span class="service-logo-fallback">${fallback}</span>`;
    }

    return `
        <img src="${logoUrl}" alt="${serviceName} logo" class="service-logo" loading="lazy" referrerpolicy="no-referrer"
             onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">
        <span class="service-logo-fallback" style="display:none;">${fallback}</span>
    `;
}

function buildOpenAction(name, url) {
    const normalizedUrl = normalizeUrl(url);
    return `<a href="${normalizedUrl}" target="oneconnect_external" rel="noopener noreferrer" class="btn-open" title="Opens in external tab, OneConnect stays open">Open</a>`;
}

document.addEventListener('DOMContentLoaded', async () => {
    const userId = localStorage.getItem('userId');
    if (!userId) return;

    loadFavourites(userId);
});

async function loadFavourites(userId) {
    const grid = document.getElementById('favouritesGrid');
    try {
        const response = await fetch(`${API_BASE_FAV}/favourites?userId=${userId}`);
        const favourites = await response.json();

        if (favourites.length === 0) {
            grid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1;">
                    <i class="fa-regular fa-heart"></i>
                    <h3>No favourites yet</h3>
                    <p>Go to the dashboard and click the heart icon to save services here.</p>
                </div>
            `;
            return;
        }

        grid.innerHTML = '';
        favourites.forEach(fav => {
            const card = document.createElement('div');
            card.className = 'service-card';
            card.innerHTML = `
                <div class="card-icon">${buildServiceIcon(fav.name, fav.url)}</div>
                <div class="card-title">${fav.name}</div>
                <div class="card-desc">Saved to favourites</div>
                <div class="card-actions">
                    ${buildOpenAction(fav.name, fav.url)}
                    <button class="btn-danger" style="flex: 0; padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; color: white; display: flex; justify-content: center; align-items: center;" onclick="removeFavourite('${fav.name}')" title="Remove from favourites">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            `;
            grid.appendChild(card);
        });
    } catch (error) {
        console.error("Error loading favourites:", error);
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1;">
                <i class="fa-solid fa-triangle-exclamation"></i>
                <p>Failed to load favourites. Please try again later.</p>
            </div>
        `;
    }
}

async function removeFavourite(serviceName) {
    const userId = localStorage.getItem('userId');
    try {
        const response = await fetch(`${API_BASE_FAV}/favourites`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId, serviceName })
        });
        
        const data = await response.json();
        if (data.success) {
            // Reload grid
            loadFavourites(userId);
        } else {
            alert('Failed to remove favourite: ' + data.message);
        }
    } catch (error) {
        console.error("Error removing favourite:", error);
    }
}
