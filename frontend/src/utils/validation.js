export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

export const validatePassword = (password) => {
  return password && password.length >= 6;
};

export const validateName = (name) => {
  return name && name.trim().length >= 2;
};

export const getValidationErrors = (formData, isLogin = true) => {
  const errors = {};

  if (!formData.email || !validateEmail(formData.email)) {
    errors.email = 'Please enter a valid email address';
  }

  if (!formData.password || !validatePassword(formData.password)) {
    errors.password = 'Password must be at least 6 characters long';
  }

  if (!isLogin) {
    if (!formData.fullName || !validateName(formData.fullName)) {
      errors.fullName = 'Full name must be at least 2 characters long';
    }

    if (formData.phone && formData.phone.length > 0 && formData.phone.length < 10) {
      errors.phone = 'Please enter a valid phone number';
    }
  }

  return errors;
};
