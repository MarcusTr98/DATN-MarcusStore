const FALLBACK_ID = 'marcus-zalo-personal-btn'

export function injectFallbackScript() {
  if (document.getElementById(FALLBACK_ID)) return

  const ZALO_PHONE = '0907640098'

  const zaloBtn = document.createElement('a')
  zaloBtn.id = FALLBACK_ID
  zaloBtn.href = `https://zalo.me/${ZALO_PHONE}`
  zaloBtn.target = '_blank'
  zaloBtn.className = 'zalo-personal-btn'

  // Dùng logo Zalo chuẩn
  zaloBtn.innerHTML = `
    <div class="zalo-icon-wrapper">
      <img src="https://upload.wikimedia.org/wikipedia/commons/9/91/Icon_of_Zalo.svg" style="width:35px;height:35px;" />
    </div>
  `

  const style = document.createElement('style')
  style.id = FALLBACK_ID + '-style'
  style.innerHTML = `
    .zalo-personal-btn {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 9999;
      cursor: pointer;
    }
    .zalo-icon-wrapper {
      width: 60px;
      height: 60px;
      background: #0068ff;
      border-radius: 50%;
      box-shadow: 0 4px 15px rgba(0, 104, 255, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: transform 0.3s ease;
    }
    .zalo-personal-btn:hover .zalo-icon-wrapper {
      transform: scale(1.1);
    }
  `
  document.head.appendChild(style)
  document.body.appendChild(zaloBtn)
}

export function removeFallbackScript() {
  document.getElementById(FALLBACK_ID)?.remove()
  document.getElementById(FALLBACK_ID + '-style')?.remove()
}
