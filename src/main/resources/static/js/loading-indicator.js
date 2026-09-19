(() => {
	const messages = [
		'Working on your request...',
		'Preparing the response...',
		'Still working...',
		'Almost ready...'
	];
	const indicator = document.getElementById('thinking-indicator');
	const message = document.getElementById('thinking-message');
	const elapsed = document.getElementById('thinking-elapsed');
	let startedAt = null;
	let intervalId = null;

	function update() {
		if (startedAt === null) return;
		const milliseconds = Date.now() - startedAt;
		const seconds = Math.max(1, Math.floor(milliseconds / 1000));
		const minutes = Math.floor(seconds / 60);
		const remainingSeconds = seconds % 60;
		const index = Math.min(messages.length - 1, Math.floor(milliseconds / 4000));
		message.textContent = messages[index];
		elapsed.textContent = minutes > 0
			? `Working for ${minutes}m ${remainingSeconds}s`
			: `Working for ${seconds}s`;
	}

	window.financeLoadingIndicator = {
		start({ statusElement } = {}) {
			this.stop({ preserveStatus: true });
			startedAt = Date.now();
			indicator.hidden = false;
			if (statusElement) statusElement.textContent = '';
			update();
			intervalId = window.setInterval(update, 1000);
		},
		stop({ preserveStatus = false } = {}) {
			if (intervalId !== null) window.clearInterval(intervalId);
			intervalId = null;
			startedAt = null;
			indicator.hidden = true;
			if (!preserveStatus) {
				elapsed.textContent = 'Starting...';
				message.textContent = messages[0];
			}
		},
		update
	};
})();
