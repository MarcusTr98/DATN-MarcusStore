const openPanels = new Set()
export function setFloatingContactPanelOpen(panel, isOpen) {
  if (isOpen) {
    openPanels.add(panel)
  } else {
    openPanels.delete(panel)
  }
  document.body.classList.toggle('marcus-contact-panel-open', openPanels.size > 0)
}

export function clearFloatingContactPanel(panel) {
  openPanels.delete(panel)
  document.body.classList.toggle('marcus-contact-panel-open', openPanels.size > 0)
}
