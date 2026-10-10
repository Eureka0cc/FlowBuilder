declare namespace API {
  type App = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type AppAddRequest = {
    initPrompt?: string
  }

  type AppAdminUpdateRequest = {
    id?: string
    appName?: string
    cover?: string
    priority?: number
  }

  type AppDeployRequest = {
    appId?: string
  }

  type AppQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    priority?: number
    userId?: string
  }

  type AppUpdateRequest = {
    id?: string
    appName?: string
  }

  type AppVO = {
    id?: string
    appName?: string
    cover?: string
    initPrompt?: string
    codeGenType?: string
    deployKey?: string
    deployedTime?: string
    priority?: number
    userId?: string
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type BaseResponseAppVO = {
    code?: number
    data?: AppVO
    message?: string
  }

  type CaptchaVO = {
    captchaId?: string
    backgroundImage?: string
    sliderImage?: string
    sliderY?: number
    expiresIn?: number
  }

  type CaptchaVerifyRequest = {
    captchaId?: string
    sliderX?: number
  }

  type BaseResponseCaptchaVO = {
    code?: number
    data?: CaptchaVO
    message?: string
  }

  type VueBuildStatusVO = {
    status?: string
    ready?: boolean
    message?: string
    timestamp?: number
  }

  type BaseResponseVueBuildStatusVO = {
    code?: number
    data?: VueBuildStatusVO
    message?: string
  }

  type getVuePreviewBuildStatusParams = {
    appId: string
  }

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponsePageAppVO = {
    code?: number
    data?: PageAppVO
    message?: string
  }

  type BaseResponsePageChatHistory = {
    code?: number
    data?: PageChatHistory
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type ChatHistory = {
    id?: string
    message?: string
    messageType?: string
    appId?: string
    userId?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type ChatHistoryQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    message?: string
    messageType?: string
    appId?: string
    userId?: string
    lastCreateTime?: string
  }

  type chatToGenCodeParams = {
    appId: string
    message: string
  }

  type DeleteRequest = {
    id?: string
  }

  type downloadAppCodeParams = {
    appId: string
  }

  type getAppVOByIdByAdminParams = {
    id: string
  }

  type getAppVOByIdParams = {
    id: string
  }

  type getInfo1Params = {
    id: string
  }

  type getInfo2Params = {
    id: string
  }

  type getInfoParams = {
    id: string
  }

  type getUserByIdParams = {
    id: string
  }

  type getUserVOByIdParams = {
    id: string
  }

  type listAppChatHistoryParams = {
    appId: string
    pageSize?: number
    lastCreateTime?: string
  }

  type LoginUserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    planCode?: string
    creditDisplay?: number
    createTime?: string
    updateTime?: string
  }

  type page1Params = {
    page: PageChatHistory
  }

  type page2Params = {
    page: PageApp
  }

  type PageApp = {
    records?: App[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageAppVO = {
    records?: AppVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageChatHistory = {
    records?: ChatHistory[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type pageParams = {
    page: PageUser
  }

  type PageUser = {
    records?: User[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type PageUserVO = {
    records?: UserVO[]
    pageNumber?: number
    pageSize?: number
    totalPage?: number
    totalRow?: number
    optimizeCountQuery?: boolean
  }

  type remove1Params = {
    id: string
  }

  type remove2Params = {
    id: string
  }

  type removeParams = {
    id: string
  }

  type ServerSentEventString = true

  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    editTime?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    pageNum?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userName?: string
    userAccount?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: string
    userAccount?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    planCode?: string
    creditDisplay?: number
    createTime?: string
  }

  type BillingAccountVO = {
    planCode?: string
    planName?: string
    planDescription?: string
    creditBalance?: number
    creditDisplay?: number
    monthlyQuota?: number
    appLimit?: number
    appCount?: number
    chatRate?: number
    planExpireTime?: string
    monthlyQuotaResetAt?: string
    creditUpdatedTime?: string
    plans?: PlanOptionVO[]
  }

  type PlanOptionVO = {
    planCode?: string
    name?: string
    description?: string
    monthlyQuota?: number
    monthlyQuotaDisplay?: number
    appLimit?: number
    chatRate?: number
    signupGift?: number
    signupGiftDisplay?: number
  }

  type CreditLedgerVO = {
    id?: string
    userId?: string
    changeAmount?: number
    changeDisplay?: number
    balanceAfter?: number
    balanceAfterDisplay?: number
    bizType?: string
    bizId?: string
    remark?: string
    operatorId?: string
    createTime?: string
  }

  type AdminCreditRechargeRequest = {
    userId?: string | number
    amount?: number
    remark?: string
  }

  type AdminCreditAdjustRequest = {
    userId?: string | number
    amount?: number
    remark?: string
  }

  type AdminAssignPlanRequest = {
    userId?: string | number
    planCode?: string
    planExpireTime?: string
    resetMonthlyQuota?: boolean
  }

  type BaseResponseBillingAccountVO = {
    code?: number
    data?: BillingAccountVO
    message?: string
  }

  type BaseResponseListPlanOptionVO = {
    code?: number
    data?: PlanOptionVO[]
    message?: string
  }

  type BaseResponsePageCreditLedgerVO = {
    code?: number
    data?: {
      records?: CreditLedgerVO[]
      pageNumber?: number
      pageSize?: number
      totalRow?: number
    }
    message?: string
  }

  type BaseResponseBigDecimal = {
    code?: number
    data?: number
    message?: string
  }
}
