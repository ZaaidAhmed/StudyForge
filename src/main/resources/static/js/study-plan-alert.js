document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.study-plan-alert.success').forEach(function (alert) {
        window.setTimeout(function () {
            alert.classList.add('is-dismissing');
            window.setTimeout(function () {
                alert.hidden = true;
            }, 180);
        }, 3000);
    });
});
