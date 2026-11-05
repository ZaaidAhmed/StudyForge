document.addEventListener('DOMContentLoaded', function () {
    var form = document.getElementById('agent-chat-form');
    var input = document.getElementById('agent-request');
    var conversation = document.querySelector('.agent-conversation');
    if (!form || !input) return;

    document.querySelectorAll('[data-agent-prompt]').forEach(function (button) {
        button.addEventListener('click', function () {
            input.value = button.dataset.agentPrompt || '';
            form.requestSubmit();
        });
    });

    input.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            if (input.value.trim()) form.requestSubmit();
        }
    });

    if (conversation && conversation.querySelector('.agent-message-pair')) {
        conversation.scrollTop = conversation.scrollHeight;
    }
});
