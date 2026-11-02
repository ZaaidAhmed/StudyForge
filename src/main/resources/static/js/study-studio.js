document.addEventListener('DOMContentLoaded', function () {
    const workspace = document.querySelector('.studio-workspace');
    const main = document.querySelector('.study-studio-content');
    if (!workspace || !main) return;

    const pageCount = Math.max(1, Number.parseInt(main.dataset.pageCount, 10) || 1);
    const pageUrl = main.dataset.pageUrl;
    const pageLabel = document.getElementById('studio-page-label');
    const pageInput = document.getElementById('studio-reader-page-input');
    const documentPages = document.getElementById('studio-document-pages');
    const documentFrame = document.querySelector('.studio-document-frame');
    const pageError = document.getElementById('studio-page-error');
    const previousPageButton = document.getElementById('studio-page-previous');
    const nextPageButton = document.getElementById('studio-page-next');
    const canNavigatePages = !previousPageButton.disabled;
    const conversation = document.querySelector('.studio-conversation');
    const chatKey = main.dataset.chatKey;
    let currentPage = Math.min(Math.max(1, Number.parseInt(main.dataset.initialPage, 10) || 1), pageCount);

    function scrollToDocumentPage(target) {
        if (!documentFrame || !target) return;
        const move = function () {
            window.requestAnimationFrame(function () {
                const frameRect = documentFrame.getBoundingClientRect();
                const targetRect = target.getBoundingClientRect();
                const top = documentFrame.scrollTop + targetRect.top - frameRect.top - 12;
                documentFrame.scrollTo({top: Math.max(0, top), behavior: 'smooth'});
                // Recalculate once more after nearby lazy pages settle their heights.
                window.setTimeout(function () {
                    const refreshedFrame = documentFrame.getBoundingClientRect();
                    const refreshedTarget = target.getBoundingClientRect();
                    const refreshedTop = documentFrame.scrollTop + refreshedTarget.top - refreshedFrame.top - 12;
                    documentFrame.scrollTo({top: Math.max(0, refreshedTop), behavior: 'smooth'});
                }, 180);
            });
        };
        const image = target.querySelector('img');
        if (image && !image.complete) {
            image.loading = 'eager';
            image.addEventListener('load', move, {once: true});
            image.addEventListener('error', move, {once: true});
        } else {
            move();
        }
    }

    function setPage(page, shouldScroll, updateUrl) {
        currentPage = Math.min(Math.max(1, page), pageCount);
        pageLabel.textContent = 'Page ' + currentPage + ' of ' + pageCount;
        pageInput.value = String(currentPage);
        if (updateUrl !== false) {
            const pageUrlState = new URL(window.location.href);
            pageUrlState.searchParams.set('page', String(currentPage));
            window.history.replaceState({}, '', pageUrlState.toString());
        }
        if (documentPages) {
            const target = documentPages.querySelector('[data-document-page="' + currentPage + '"]');
            documentPages.querySelectorAll('.is-cited-page').forEach(function (page) {
                page.classList.remove('is-cited-page');
            });
            if (target && shouldScroll !== false) {
                target.classList.add('is-cited-page');
                scrollToDocumentPage(target);
            } else if (target) {
                target.classList.add('is-cited-page');
            }
        }
        previousPageButton.disabled = !canNavigatePages || currentPage <= 1;
        nextPageButton.disabled = !canNavigatePages || currentPage >= pageCount;
    }

    if (documentPages && pageUrl) {
        for (let page = 1; page <= pageCount; page += 1) {
            const wrapper = document.createElement('figure');
            wrapper.className = 'studio-document-page-wrapper';
            wrapper.dataset.documentPage = String(page);
            const image = document.createElement('img');
            image.className = 'studio-document-page';
            image.loading = page === 1 ? 'eager' : 'lazy';
            image.src = pageUrl + '?page=' + encodeURIComponent(page);
            image.alt = 'Page ' + page + ' of ' + pageCount + ' from the selected document';
            image.addEventListener('error', function () {
                wrapper.classList.add('is-error');
                image.alt = 'Page ' + page + ' could not be loaded';
            });
            const pageNumber = document.createElement('figcaption');
            pageNumber.className = 'studio-document-page-number';
            pageNumber.textContent = 'Page ' + page + ' of ' + pageCount;
            wrapper.appendChild(pageNumber);
            wrapper.appendChild(image);
            documentPages.appendChild(wrapper);
        }
    }

    const hasExplicitPage = new URL(window.location.href).searchParams.has('page');
    // Do not move the reader during normal page load or after a question submit.
    // Only an explicit page URL, citation, or page-control action may scroll it.
    setPage(currentPage, hasExplicitPage, false);

    // Keep the submitted page aligned with what the reader is actually showing.
    // Otherwise asking a question after scrolling can reload the document at an
    // older page and appear to move the reader unexpectedly.
    if (documentFrame && documentPages && 'IntersectionObserver' in window) {
        const pageObserver = new IntersectionObserver(function (entries) {
            const visible = entries.filter(function (entry) { return entry.isIntersecting; })
                .sort(function (left, right) { return right.intersectionRatio - left.intersectionRatio; })[0];
            if (visible) {
                const visiblePage = Number.parseInt(visible.target.dataset.documentPage, 10) || 1;
                if (visiblePage !== currentPage) setPage(visiblePage, false, false);
            }
        }, {root: documentFrame, threshold: [0.55, 0.75]});
        documentPages.querySelectorAll('[data-document-page]').forEach(function (page) { pageObserver.observe(page); });
    }

    if (pageError) {
        pageError.hidden = true;
    }

    // Render the limited Markdown-like structure commonly returned by the
    // tutor without using innerHTML or trusting model output. Every value is
    // inserted as a text node, so snippets and answers remain escaped.
    const answerText = document.getElementById('studio-answer-text');
    if (answerText) {
        const rawAnswer = answerText.textContent || '';
        const lines = rawAnswer.split(/\r?\n/);
        const fragment = document.createDocumentFragment();
        let list = null;
        function finishList() { if (list) { fragment.appendChild(list); list = null; } }
        lines.forEach(function (line) {
            const trimmed = line.trim();
            if (!trimmed) { finishList(); return; }
            if (/^[-*•]\s+/.test(trimmed)) {
                if (!list) { list = document.createElement('ul'); }
                const item = document.createElement('li');
                item.textContent = trimmed.replace(/^[-*•]\s+/, '');
                list.appendChild(item);
                return;
            }
            finishList();
            if (trimmed.includes('|') && !/^\|?\s*:?-{2,}/.test(trimmed)) {
                const row = document.createElement('p');
                row.className = 'studio-answer-table-row';
                trimmed.split('|').map(function (cell) { return cell.trim(); }).filter(Boolean).forEach(function (cell, index) {
                    const span = document.createElement('span');
                    span.textContent = cell;
                    if (index > 0) span.className = 'studio-answer-table-cell';
                    row.appendChild(span);
                });
                fragment.appendChild(row);
                return;
            }
            const paragraph = document.createElement('p');
            paragraph.textContent = trimmed;
            fragment.appendChild(paragraph);
        });
        finishList();
        answerText.replaceChildren(fragment);
    }

    function loadChatHistory() {
        if (!chatKey) return [];
        try {
            const stored = JSON.parse(window.localStorage.getItem(chatKey) || '[]');
            return Array.isArray(stored) ? stored.slice(-20) : [];
        } catch (error) { return []; }
    }

    function saveChatHistory(history) {
        if (!chatKey) return;
        try { window.localStorage.setItem(chatKey, JSON.stringify(history.slice(-20))); } catch (error) { /* optional storage */ }
    }

    function readCurrentTurn() {
        const questionNode = document.querySelector('.studio-user-message .studio-message-bubble p');
        if (!questionNode) return null;
        const answerNode = document.querySelector('.studio-answer-card .studio-answer-text');
        const errorNode = document.querySelector('.studio-chat-error');
        const sources = Array.from(document.querySelectorAll('.studio-answer-card .studio-source-card')).map(function (source) {
            const children = source.querySelectorAll('span');
            return {page: Number.parseInt(source.dataset.citationPage, 10) || 1,
                reference: children[0] ? children[0].textContent : '',
                snippet: children[1] ? children[1].textContent : ''};
        });
        return {question: questionNode.textContent || '', answer: answerNode ? answerNode.textContent || '' : '',
            error: errorNode ? errorNode.textContent || '' : '', sources: sources};
    }

    function sameTurn(left, right) {
        return left && right && left.question === right.question && left.answer === right.answer && left.error === right.error;
    }

    function makeMessage(className) {
        const message = document.createElement('div');
        message.className = 'studio-message ' + className;
        return message;
    }

    function renderHistoryTurn(turn, container) {
        const user = makeMessage('studio-user-message');
        const bubble = document.createElement('div'); bubble.className = 'studio-message-bubble';
        const label = document.createElement('span'); label.className = 'studio-message-label'; label.textContent = 'You';
        const question = document.createElement('p'); question.textContent = turn.question || '';
        bubble.append(label, question); user.appendChild(bubble); container.appendChild(user);

        const tutor = makeMessage('studio-tutor-message');
        if (turn.error) {
            const error = document.createElement('div'); error.className = 'studio-chat-error'; error.setAttribute('role', 'alert'); error.textContent = turn.error;
            tutor.appendChild(error);
        } else {
            tutor.classList.add('studio-answer-card');
            const answer = document.createElement('div'); answer.className = 'studio-answer-text'; answer.textContent = turn.answer || 'No answer was returned.';
            tutor.appendChild(answer);
            if (turn.sources && turn.sources.length) {
                const sources = document.createElement('div'); sources.className = 'studio-sources';
                const heading = document.createElement('div'); heading.className = 'studio-section-label';
                const strong = document.createElement('strong'); strong.textContent = 'Supporting evidence';
                const count = document.createElement('span'); count.textContent = turn.sources.length + ' source(s)';
                heading.append(strong, count); sources.appendChild(heading);
                turn.sources.forEach(function (item) {
                    const source = document.createElement('button'); source.type = 'button'; source.className = 'studio-source-card'; source.dataset.citationPage = String(item.page || 1);
                    const reference = document.createElement('span'); reference.className = 'studio-source-reference'; reference.textContent = item.reference || ('Page ' + (item.page || 1));
                    const snippet = document.createElement('span'); snippet.textContent = item.snippet || '';
                    source.append(reference, snippet); sources.appendChild(source); bindCitation(source);
                });
                tutor.appendChild(sources);
            }
        }
        container.appendChild(tutor);
    }

    function renderChatHistory() {
        if (!conversation || !chatKey) return;
        let history = loadChatHistory();
        const current = readCurrentTurn();
        if (current && (!history.length || !sameTurn(history[history.length - 1], current))) {
            history.push(current); saveChatHistory(history);
        }
        if (!history.length) return;
        const turnsToRender = current && sameTurn(history[history.length - 1], current) ? history.slice(0, -1) : history;
        if (!turnsToRender.length) return;
        const historyContainer = document.createElement('div'); historyContainer.className = 'studio-history';
        turnsToRender.forEach(function (turn) { renderHistoryTurn(turn, historyContainer); });
        const welcome = conversation.querySelector('.studio-welcome'); if (welcome) welcome.hidden = true;
        conversation.insertBefore(historyContainer, conversation.firstChild);
    }

    previousPageButton.addEventListener('click', function () {
        setPage(currentPage - 1);
    });
    nextPageButton.addEventListener('click', function () {
        setPage(currentPage + 1);
    });

    function bindCitation(citation) {
        citation.addEventListener('click', function () {
            setPage(Number.parseInt(citation.dataset.citationPage, 10) || 1);
        });
    }
    document.querySelectorAll('[data-citation-page]').forEach(bindCitation);
    renderChatHistory();

    const clearChat = document.getElementById('studio-clear-chat');
    if (clearChat) clearChat.addEventListener('click', function () {
        try { window.localStorage.removeItem(chatKey); } catch (error) { /* optional storage */ }
        // Reload the clean GET view so the current server-rendered answer,
        // active citation, and stored browser history are all removed together.
        // Preserve the page currently being viewed so clearing chat never moves
        // the document reader unexpectedly.
        const cleanUrl = new URL(window.location.href);
        const visiblePage = pageInput && pageInput.value;
        if (visiblePage) cleanUrl.searchParams.set('page', visiblePage);
        window.location.assign(cleanUrl.toString());
    });

    document.getElementById('studio-document-select').addEventListener('change', function (event) {
        const option = event.target.selectedOptions[0];
        if (option && option.dataset.url) window.location.assign(option.dataset.url);
    });

    document.querySelectorAll('.studio-mode-button').forEach(function (button) {
        button.addEventListener('click', function () {
            const mode = button.dataset.viewMode;
            workspace.dataset.viewMode = mode;
            document.querySelectorAll('.studio-mode-button').forEach(function (modeButton) {
                const active = modeButton === button;
                modeButton.classList.toggle('is-active', active);
                modeButton.setAttribute('aria-pressed', String(active));
            });
        });
    });

    function activateTab(tab) {
        document.querySelectorAll('[data-studio-tab]').forEach(function (tabButton) {
            const selected = tabButton === tab;
            tabButton.setAttribute('aria-selected', String(selected));
            document.getElementById(tabButton.dataset.studioTab).hidden = !selected;
        });
    }

    document.querySelectorAll('[data-studio-tab]').forEach(function (tab) {
        tab.addEventListener('click', function () {
            activateTab(tab);
        });
    });

    document.querySelectorAll('[data-document-question]').forEach(function (button) {
        button.addEventListener('click', function () {
            const question = document.getElementById('document-question');
            question.value = button.dataset.documentQuestion;
            question.focus();
        });
    });

    const noteKey = main.dataset.notesKey;
    const noteInput = document.getElementById('studio-note-input');
    const noteList = document.getElementById('studio-note-list');
    const noteStatus = document.getElementById('studio-note-status');
    const addNoteButton = document.getElementById('studio-add-note');
    let notes = [];
    let canSaveNotes = true;

    function showNotesUnavailable(message) {
        canSaveNotes = false;
        addNoteButton.disabled = true;
        noteStatus.textContent = message;
    }

    function renderNotes() {
        noteList.replaceChildren();
        if (notes.length === 0) {
            const empty = document.createElement('p');
            empty.className = 'studio-note-storage';
            empty.textContent = 'Your notes for this document will appear here.';
            noteList.appendChild(empty);
            return;
        }
        notes.forEach(function (note) {
            const card = document.createElement('article');
            card.className = 'studio-note-card';
            card.appendChild(document.createTextNode(note.text));
            const date = document.createElement('span');
            date.className = 'studio-note-date';
            date.textContent = new Date(note.createdAt).toLocaleString();
            card.appendChild(date);
            noteList.appendChild(card);
        });
    }

    try {
        const savedNotes = window.localStorage.getItem(noteKey);
        const parsedNotes = savedNotes ? JSON.parse(savedNotes) : [];
        if (!Array.isArray(parsedNotes) || parsedNotes.some(function (note) {
            return !note || typeof note.text !== 'string' || typeof note.createdAt !== 'string';
        })) {
            showNotesUnavailable('Saved notes could not be read because their stored format is invalid.');
        } else {
            notes = parsedNotes;
        }
    } catch (error) {
        showNotesUnavailable('Browser storage is unavailable. Notes cannot be loaded or saved.');
    }
    renderNotes();

    addNoteButton.addEventListener('click', function () {
        const text = noteInput.value.trim();
        if (!text) {
            noteStatus.textContent = 'Write a note before saving.';
            noteInput.focus();
            return;
        }
        if (!canSaveNotes) return;
        const updatedNotes = notes.concat({text: text, createdAt: new Date().toISOString()});
        try {
            window.localStorage.setItem(noteKey, JSON.stringify(updatedNotes));
            notes = updatedNotes;
            noteInput.value = '';
            noteStatus.textContent = 'Note saved in this browser.';
            renderNotes();
        } catch (error) {
            noteStatus.textContent = 'The note could not be saved because browser storage is unavailable.';
        }
    });

    const saveAnswerButton = document.getElementById('studio-save-answer');
    if (saveAnswerButton) {
        saveAnswerButton.addEventListener('click', function () {
            const answer = document.getElementById('studio-answer-text');
            noteInput.value = answer.textContent.trim();
            activateTab(document.getElementById('studio-notes-tab'));
            noteInput.focus();
        });
    }

    const chatForm = document.getElementById('document-chat-form');
    if (conversation && document.querySelector('.studio-user-message')) {
        window.requestAnimationFrame(function () {
            conversation.scrollTop = conversation.scrollHeight;
        });
    }
    chatForm.addEventListener('submit', function () {
        const submitButton = document.getElementById('document-ask-submit');
        const status = document.getElementById('document-chat-status');
        submitButton.disabled = true;
        submitButton.textContent = 'Thinking…';
        status.textContent = 'Searching this document and preparing an answer.';
    });
});
