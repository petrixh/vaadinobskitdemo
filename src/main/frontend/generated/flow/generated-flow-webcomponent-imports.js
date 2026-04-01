import { injectGlobalWebcomponentCss } from 'Frontend/generated/jar-resources/theme-util.js';

import '@vaadin/common-frontend/ConnectionIndicator.js';
import 'Frontend/generated/jar-resources/ReactRouterOutletElement.tsx';
import 'react-router';
import 'react';

const loadOnDemand = (key) => {
  const pending = [];
  if (key === 'b0624c5e72cb18c651eba8818bfa3ae9ddec8c368d60d641eb59f28c2f50476a') {
    pending.push(import('./chunks/chunk-1d76b1786f4efcf0d49b6daba1c993e64b06339b14b71e5dbbe55c5d10bdee65.js'));
  }
  if (key === '26f01a8ed2303c2562b27a0d2f62763e94720953f6d309312270b058f189ae2d') {
    pending.push(import('./chunks/chunk-ff295cfd0e1e1052ea288cb7a92fa9f509e29dc226a913afcd16d06f8261cc28.js'));
  }
  if (key === '997e5c892f9c322a8d715608955ef01775c73670fefc1f8d90ed30efa4206ab9') {
    pending.push(import('./chunks/chunk-1d76b1786f4efcf0d49b6daba1c993e64b06339b14b71e5dbbe55c5d10bdee65.js'));
  }
  if (key === '0f637bd6f6aead105ad9b0b9dc83b8f7947d84f00114548df7f1aa3819f7a0c8') {
    pending.push(import('./chunks/chunk-6cc839071d1a61e72f7f2bcbedcea54c17e3df6f30e3afac78e42d886219bd15.js'));
  }
  if (key === '6a1812b4f9abfc0d5543d65e2d919ab1c2e4c14ad54aa768f82d208097837e1f') {
    pending.push(import('./chunks/chunk-1f9c6be1d08b773e0518ddb5e4a3d187926736d5fee631cb8ee31c9e42c840e2.js'));
  }
  if (key === 'bea33ef4b0ce9e1b8246cf691eed984fcc6455a695258d32f5f0a5cd3f7879ff') {
    pending.push(import('./chunks/chunk-1f9c6be1d08b773e0518ddb5e4a3d187926736d5fee631cb8ee31c9e42c840e2.js'));
  }
  return Promise.all(pending);
}
window.Vaadin = window.Vaadin || {};
window.Vaadin.Flow = window.Vaadin.Flow || {};
window.Vaadin.Flow.loadOnDemand = loadOnDemand;
window.Vaadin.Flow.resetFocus = () => {
 let ae=document.activeElement;
 while(ae&&ae.shadowRoot) ae = ae.shadowRoot.activeElement;
 return !ae || ae.blur() || ae.focus() || true;
}