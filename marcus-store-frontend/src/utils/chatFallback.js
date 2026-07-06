export const FLOATING_CONTAINER_ID = 'marcus-floating-actions'

const ZALO_ID = 'marcus-zalo-btn'
const FB_ID = 'marcus-fb-btn'
const ZALO_PHONE = '0907640098'
const FB_PAGE_USERNAME = 'Mr.Bear2202'

function ensureContainer() {
  if (document.getElementById(FLOATING_CONTAINER_ID)) return

  const container = document.createElement('div')
  container.id = FLOATING_CONTAINER_ID
  document.body.appendChild(container)

  const style = document.createElement('style')
  style.id = FLOATING_CONTAINER_ID + '-style'
  style.innerHTML = `
    #${FLOATING_CONTAINER_ID} {
      position: fixed;
      bottom: 24px;
      right: 24px;
      z-index: 1040;
      display: flex;
      flex-direction: column; /* Xếp dọc từ trên xuống dưới */
      align-items: center;
      gap: 16px;
    }

    .marcus-float-btn {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      text-decoration: none;
      position: relative;
    }

    /* Hiệu ứng Tooltip */
    .marcus-float-btn::before {
      content: attr(data-tooltip);
      position: absolute;
      right: 75px;
      top: 50%;
      transform: translateY(-50%);
      background: rgba(0, 0, 0, 0.75);
      color: #fff;
      padding: 6px 14px;
      border-radius: 8px;
      font-size: 13px;
      font-weight: 500;
      white-space: nowrap;
      opacity: 0;
      visibility: hidden;
      transition: all 0.3s ease;
      pointer-events: none;
      font-family: 'Be Vietnam Pro', sans-serif;
    }
    .marcus-float-btn:hover::before {
      opacity: 1;
      visibility: visible;
      right: 70px;
    }

    /* Hiệu ứng Rung (Shake) khi di chuột */
    @keyframes float-shake {
      0% { transform: translateY(0) rotate(0deg); }
      25% { transform: translateY(-4px) rotate(-6deg); }
      50% { transform: translateY(-4px) rotate(6deg); }
      75% { transform: translateY(-4px) rotate(-6deg); }
      100% { transform: translateY(0) rotate(0deg); }
    }
    .marcus-float-btn:hover {
      animation: float-shake 0.4s ease-in-out infinite;
    }

    .zalo-float-btn {
      background: #0068ff;
      box-shadow: 0 4px 15px rgba(0, 104, 255, 0.35);
    }

    .fb-float-btn {
      background: linear-gradient(135deg, #00c6ff, #0078ff);
      box-shadow: 0 4px 15px rgba(0, 120, 255, 0.35);
    }
  `
  document.head.appendChild(style)
}

function injectFacebookButton() {
  if (document.getElementById(FB_ID)) return

  const fbBtn = document.createElement('a')
  fbBtn.id = FB_ID
  fbBtn.href = `https://m.me/${FB_PAGE_USERNAME}`
  fbBtn.target = '_blank'
  fbBtn.rel = 'noopener'
  fbBtn.className = 'marcus-float-btn fb-float-btn'
  fbBtn.setAttribute('data-tooltip', 'Chat qua Messenger') // Truyền text cho Tooltip
  fbBtn.innerHTML = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 36 36" width="32" height="32" fill="#fff">
      <path d="M18 0C7.94 0 0 7.5 0 17.14c0 5.49 2.58 10.39 6.6 13.58V36l6.03-3.31c1.61.44 3.32.68 5.37.68 10.06 0 18-7.5 18-17.14C36 7.5 28.06 0 18 0zm1.79 23.08l-4.59-4.9-8.96 4.9 9.85-10.47 4.7 4.9 8.85-4.9-9.85 10.47z"/>
    </svg>
  `
  document.getElementById(FLOATING_CONTAINER_ID).appendChild(fbBtn)
}

function injectZaloButton() {
  if (document.getElementById(ZALO_ID)) return

  const zaloBtn = document.createElement('a')
  zaloBtn.id = ZALO_ID
  zaloBtn.href = `https://zalo.me/${ZALO_PHONE}`
  zaloBtn.target = '_blank'
  zaloBtn.rel = 'noopener'
  zaloBtn.className = 'marcus-float-btn zalo-float-btn'
  zaloBtn.setAttribute('data-tooltip', 'Chat qua Zalo') // Truyền text cho Tooltip
  zaloBtn.innerHTML = `<img src="https://upload.wikimedia.org/wikipedia/commons/9/91/Icon_of_Zalo.svg" style="width:34px;height:34px;" alt="Zalo" />`

  document.getElementById(FLOATING_CONTAINER_ID).appendChild(zaloBtn)
}

export function injectFallbackScript() {
  ensureContainer()
  injectFacebookButton() // Gọi FB trước để nó nằm trên Zalo
  injectZaloButton() // Gọi Zalo sau để nó nằm dưới cùng
}

export function removeFallbackScript() {
  document.getElementById(ZALO_ID)?.remove()
  document.getElementById(FB_ID)?.remove()
  document.getElementById(FLOATING_CONTAINER_ID)?.remove()
  document.getElementById(FLOATING_CONTAINER_ID + '-style')?.remove()
}
