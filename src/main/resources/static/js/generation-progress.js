(function () {
    'use strict';

    var OPERATION_STAGES = {
        summary: [
            [12, 'Preparing the summary'], [38, 'Reading relevant evidence'],
            [76, 'Writing a grounded summary'], [91, 'Checking source links']
        ],
        concepts: [
            [12, 'Preparing concept extraction'], [40, 'Reading relevant evidence'],
            [76, 'Structuring the concepts'], [91, 'Checking source links']
        ],
        flashcards: [
            [10, 'Preparing flashcard generation'], [32, 'Reading document evidence'],
            [74, 'Generating flashcards'], [91, 'Checking and saving cards']
        ],
        quiz: [
            [10, 'Preparing quiz generation'], [32, 'Reading document evidence'],
            [74, 'Generating grounded questions'], [91, 'Checking and saving the quiz']
        ],
        question: [
            [12, 'Finding relevant evidence'], [42, 'Checking source grounding'],
            [78, 'Preparing the answer'], [91, 'Finishing the response']
        ],
        agent: [
            [12, 'Understanding the study request'], [42, 'Planning the next steps'],
            [76, 'Running the study tools'], [91, 'Preparing the response']
        ],
        studyPlan: [
            [20, 'Checking your study plan'], [58, 'Building the schedule'],
            [91, 'Saving the study plan']
        ]
    };

    function stagesFor(operation) {
        return OPERATION_STAGES[operation] || OPERATION_STAGES.question;
    }

    function createProgress(form, operation) {
        var existing = form.parentElement.querySelector('.generation-progress, .ai-progress');
        if (existing) {
            existing.classList.add('generation-progress');
            existing.hidden = true;
            return {
                panel: existing,
                stage: existing.querySelector('[data-progress-stage], #quiz-progress-stage, #ask-status'),
                percent: existing.querySelector('[data-progress-percent], #quiz-progress-percent'),
                bar: existing.querySelector('progress, [data-progress-bar]')
            };
        }

        var panel = document.createElement('div');
        panel.className = 'generation-progress';
        panel.hidden = true;
        panel.setAttribute('role', 'status');
        panel.setAttribute('aria-live', 'polite');
        panel.innerHTML = '<div class="generation-progress-heading"><strong data-progress-stage>Preparing</strong><span data-progress-percent>0% complete</span></div>' +
            '<progress data-progress-bar max="100" value="0" aria-label="Generation progress"></progress>';
        form.insertAdjacentElement('afterend', panel);
        return {
            panel: panel,
            stage: panel.querySelector('[data-progress-stage]'),
            percent: panel.querySelector('[data-progress-percent]'),
            bar: panel.querySelector('[data-progress-bar]')
        };
    }

    function initialize(form) {
        if (form.dataset.progressReady === 'true') return;
        form.dataset.progressReady = 'true';
        var operation = form.dataset.progressOperation || 'question';
        var progress = createProgress(form, operation);
        var stages = stagesFor(operation);
        var button = form.querySelector('[type="submit"]');
        if (!button) return;
        var timers = [];

        form.addEventListener('submit', function () {
            if (form.dataset.progressRunning === 'true') return;
            form.dataset.progressRunning = 'true';
            button.disabled = true;
            button.dataset.originalLabel = button.textContent;
            button.textContent = 'Working…';
            progress.panel.hidden = false;
            function render(item) {
                var value = item[0];
                if (progress.bar) progress.bar.value = value;
                if (progress.percent) progress.percent.textContent = value + '% complete';
                if (progress.stage) progress.stage.textContent = item[1];
            }
            render(stages[0]);
            stages.slice(1).forEach(function (item, index) {
                timers.push(window.setTimeout(function () {
                    render(item);
                }, (index + 1) * 2400));
            });
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('form[data-progress-operation]').forEach(initialize);
    });
}());
