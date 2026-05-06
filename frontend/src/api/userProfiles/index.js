import api from '/plugins/interceptor.js'

const PROFILE_IMAGE_GENERATION_TIMEOUT_MS = 120000

export const getMyProfile = async () => {
  return api.get('/user-profiles/me')
}

export const updateMyProfile = async (body) => {
  return api.patch('/user-profiles/me', body)
}

export const createProfileImageUploadUrl = async (body) => {
  return api.post('/user-profiles/me/profile-image/presigned-url', body)
}

export const updateMyProfileImage = async (body) => {
  return api.patch('/user-profiles/me/profile-image', body)
}

export const deleteMyProfileImage = async () => {
  return api.delete('/user-profiles/me/profile-image')
}

export const generateMyProfileImage = async (body) => {
  return api.post('/user-profiles/me/profile-image/generate', body, {
    timeout: PROFILE_IMAGE_GENERATION_TIMEOUT_MS,
  })
}

export const getMyProfileImageHistories = async () => {
  return api.get('/user-profiles/me/profile-image/histories')
}

export const selectMyProfileImageHistory = async (body) => {
  return api.patch('/user-profiles/me/profile-image/select', body)
}

export const uploadProfileImageToS3 = async ({ uploadUrl, file, contentType }) => {
  const response = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': contentType,
    },
    body: file,
  })

  if (!response.ok) {
    throw new Error('프로필 이미지를 S3에 업로드하지 못했습니다.')
  }

  return response
}

export default {
  createProfileImageUploadUrl,
  deleteMyProfileImage,
  generateMyProfileImage,
  getMyProfile,
  getMyProfileImageHistories,
  selectMyProfileImageHistory,
  updateMyProfile,
  updateMyProfileImage,
  uploadProfileImageToS3,
}
