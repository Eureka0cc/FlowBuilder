<template>
  <a-layout-footer class="footer">
    <div class="footer-content">
      编程导航原创项目 by
      <a href="https://www.codefather.cn" target="_blank" rel="noopener noreferrer"> 程序员鱼皮 </a>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.footer {
  text-align: center;
  background: #f0f2f5;
  padding: 24px 50px;
  position: relative;
  bottom: 0;
  width: 100%;
}

.footer-content {
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.footer-content a {
  color: #1890ff;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-content a:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 576px) {
  .footer {
    padding: 16px 24px;
  }

  .footer-content {
    font-size: 12px;
  }
}
</style>
