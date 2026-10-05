// Navigation, downloads, videos, FAQ and every workflow step remain usable without JS.
const systemAppearance = matchMedia('(prefers-color-scheme: dark)');
const appearanceButton = document.querySelector('[data-appearance-toggle]');
let appearance;
try { const saved = localStorage.getItem('droprun-appearance'); if (saved === 'light' || saved === 'dark') appearance = saved; } catch {}
function updateAppearance() {
  const dark = (appearance || (systemAppearance.matches ? 'dark' : 'light')) === 'dark';
  document.documentElement.dataset.appearance = dark ? 'dark' : 'light';
  for (const source of document.querySelectorAll('[data-dark-screen]')) source.media = dark ? 'all' : 'not all';
  for (const video of document.querySelectorAll('[data-light-poster]')) video.poster = dark ? video.dataset.darkPoster : video.dataset.lightPoster;
  for (const meta of document.querySelectorAll('meta[name="theme-color"]')) meta.content = dark ? '#171B18' : '#F7F7F2';
  if (appearanceButton) {
    appearanceButton.textContent = dark ? appearanceButton.dataset.dark : appearanceButton.dataset.light;
    appearanceButton.setAttribute('aria-pressed', String(dark));
  }
}
appearanceButton?.addEventListener('click', () => {
  appearance = document.documentElement.dataset.appearance === 'dark' ? 'light' : 'dark';
  try { localStorage.setItem('droprun-appearance', appearance); } catch {}
  updateAppearance();
});
if (appearanceButton) appearanceButton.hidden = false;
systemAppearance.addEventListener?.('change', () => { if (!appearance) updateAppearance(); });
updateAppearance();

const motionPreference = matchMedia('(prefers-reduced-motion: reduce)');
const animations = new Set();
let revealObserver;

function animate(element, keyframes, duration = 280) {
  if (motionPreference.matches || typeof element.animate !== 'function') return;
  const animation = element.animate(keyframes, { duration, easing: 'cubic-bezier(.22,.8,.25,1)' });
  animations.add(animation);
  animation.finished.then(() => animations.delete(animation), () => animations.delete(animation));
}

function updateMotion() {
  revealObserver?.disconnect();
  document.documentElement.classList.remove('motion');
  for (const element of document.querySelectorAll('.reveal.waiting')) element.classList.remove('waiting');
  if (motionPreference.matches) {
    for (const animation of animations) animation.cancel();
    animations.clear();
    return;
  }
  if (!('IntersectionObserver' in window)) return;
  document.documentElement.classList.add('motion');
  revealObserver = new IntersectionObserver(entries => {
    for (const entry of entries) if (entry.isIntersecting) {
      entry.target.classList.remove('waiting');
      revealObserver.unobserve(entry.target);
    }
  }, { threshold: .08 });
  for (const element of document.querySelectorAll('.reveal')) {
    if (element.getBoundingClientRect().top > innerHeight) element.classList.add('waiting');
    revealObserver.observe(element);
  }
}
motionPreference.addEventListener?.('change', updateMotion);
updateMotion();

for (const flow of document.querySelectorAll('[data-stepper]')) {
  const tablist = flow.querySelector('[data-step-tabs]');
  const buttons = [...tablist.querySelectorAll('[data-step]')];
  const panels = [...flow.querySelectorAll('[data-step-panel]')];
  if (buttons.length !== panels.length || !buttons.length) continue;
  let selected = 0;

  function select(index, moveFocus = false, withMotion = true) {
    if (index < 0 || index >= buttons.length) return;
    for (const animation of animations) if (panels.includes(animation.effect?.target)) animation.cancel();
    const changed = index !== selected;
    selected = index;
    for (const [position, button] of buttons.entries()) {
      const active = position === index;
      button.setAttribute('aria-selected', String(active));
      button.tabIndex = active ? 0 : -1;
      panels[position].hidden = !active;
    }
    if (moveFocus) buttons[index].focus();
    if (changed && withMotion) animate(panels[index], [{ opacity: .25, transform: 'translateY(8px)' }, { opacity: 1, transform: 'translateY(0)' }]);
  }

  tablist.setAttribute('role', 'tablist');
  for (const [index, button] of buttons.entries()) {
    button.setAttribute('role', 'tab');
    button.setAttribute('aria-controls', panels[index].id);
    panels[index].setAttribute('role', 'tabpanel');
    panels[index].setAttribute('aria-labelledby', button.id);
    panels[index].tabIndex = 0;
    button.addEventListener('click', () => select(index));
    button.addEventListener('keydown', event => {
      const next = { ArrowRight: (index + 1) % buttons.length, ArrowLeft: (index + buttons.length - 1) % buttons.length, Home: 0, End: buttons.length - 1 }[event.key];
      if (next !== undefined) { event.preventDefault(); select(next, true); }
    });
  }
  select(0, false, false);
  flow.dataset.enhanced = '';
  tablist.hidden = false;
}

for (const details of document.querySelectorAll('.faq-list details')) {
  details.addEventListener('toggle', () => {
    if (details.open) animate(details.querySelector('[data-answer]'), [{ opacity: 0, transform: 'translateY(-4px)' }, { opacity: 1, transform: 'translateY(0)' }], 180);
  });
}
