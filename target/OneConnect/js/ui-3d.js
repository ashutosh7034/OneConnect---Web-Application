(function () {
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (prefersReducedMotion) {
        return;
    }

    function clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }

    function attachTilt(element, options) {
        if (!element) return;
        const maxRotateX = options.maxRotateX || 8;
        const maxRotateY = options.maxRotateY || 10;
        const scale = options.scale || 1.01;

        let rafId = null;

        function updateTransform(event) {
            const rect = element.getBoundingClientRect();
            const px = (event.clientX - rect.left) / rect.width;
            const py = (event.clientY - rect.top) / rect.height;

            const rotateY = clamp((px - 0.5) * (maxRotateY * 2), -maxRotateY, maxRotateY);
            const rotateX = clamp((0.5 - py) * (maxRotateX * 2), -maxRotateX, maxRotateX);

            element.style.transform = `perspective(1400px) rotateX(${rotateX.toFixed(2)}deg) rotateY(${rotateY.toFixed(2)}deg) scale(${scale})`;
        }

        element.addEventListener('mousemove', (event) => {
            if (rafId) cancelAnimationFrame(rafId);
            rafId = requestAnimationFrame(() => updateTransform(event));
        });

        element.addEventListener('mouseleave', () => {
            if (rafId) cancelAnimationFrame(rafId);
            element.style.transform = '';
        });
    }

    function initAuthTilt() {
        const authShell = document.querySelector('.auth-shell');
        if (authShell) {
            attachTilt(authShell, { maxRotateX: 6, maxRotateY: 8, scale: 1.008 });
        }
    }

    function initDashboardTilt() {
        const hero = document.querySelector('.dashboard-hero');
        if (hero) {
            attachTilt(hero, { maxRotateX: 5, maxRotateY: 7, scale: 1.006 });
        }

        const bindCards = () => {
            document.querySelectorAll('.service-card').forEach((card) => {
                if (card.dataset.tiltBound === '1') return;
                card.dataset.tiltBound = '1';
                attachTilt(card, { maxRotateX: 8, maxRotateY: 10, scale: 1.02 });
            });
        };

        bindCards();

        const grid = document.getElementById('servicesGrid');
        if (grid) {
            const observer = new MutationObserver(() => bindCards());
            observer.observe(grid, { childList: true, subtree: true });
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        initAuthTilt();
        initDashboardTilt();
    });
})();
