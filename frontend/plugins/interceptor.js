import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 5000,
})

api.interceptors.request.use(
  (config) => {
    console.log('요청 보내기 전에 실행')
    return config
  },
  (error) => {
    console.log('요청 보낼 때 에러 발생', error)
    return Promise.reject(error)
  },
)

api.interceptors.response.use(
  (response) => {
    console.log('응답 받아서 화면에 띄우기 전에 실행')
    return response
  },
  (error) => {
    console.log('응답 받을 때 에러 발생', error)
    return Promise.reject(error)
  },
)
export const apiFetch = async (url, options = {}) => {
  const method = (options.method || 'GET').toUpperCase()
  const data = options.body
  const headers = options.headers || {}

  const res = await api.request({
    url,
    method,
    data,
    headers,
  })

  return res.data
}
export default api
