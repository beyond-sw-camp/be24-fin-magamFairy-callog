export const formatRelativeTime = (dateString) => {
  if (!dateString) return "";

  const now = new Date();
  const past = new Date(dateString);
  const diffInMs = now - past;
  
  const diffInSeconds = Math.floor(diffInMs / 1000);
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  const diffInHours = Math.floor(diffInMinutes / 60);
  const diffInDays = Math.floor(diffInHours / 24);

  // 1. 1분 미만 (60초 미만)
  if (diffInSeconds < 60) {
    return "방금 전";
  }
  // 2. 1시간 미만 (60분 미만)
  if (diffInMinutes < 60) {
    return `${diffInMinutes}분 전`;
  }
  // 3. 24시간 미만
  if (diffInHours < 24) {
    return `${diffInHours}시간 전`;
  }
  // 4. 1개월(30일) 이내
  if (diffInDays < 30) {
    return `${diffInDays}일 전`;
  }
  // 5. 그 이후 (YYYY-MM-DD)
  return past.toISOString().split('T')[0];
};