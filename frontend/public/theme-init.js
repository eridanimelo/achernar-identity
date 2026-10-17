/* Aplica o tema salvo antes da pintura (evita flash). Sem inline por causa da CSP; mesma origem. */
(function () {
  try {
    var t = localStorage.getItem('achernar.theme');
    if (t === 'light' || t === 'dark') document.documentElement.setAttribute('data-theme', t);
    var l = localStorage.getItem('achernar.locale');
    if (l === 'pt-BR' || l === 'en') document.documentElement.setAttribute('lang', l);
  } catch (e) { /* armazenamento indisponível: segue o sistema */ }
})();
