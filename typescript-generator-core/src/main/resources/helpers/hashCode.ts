/**
 * 初始化哈希码计算
 */
function __initializeHashCode(className: string): number {
  // 使用类名的哈希值作为初始值
  let hash = 0;
  for (let i = 0; i < className.length; i++) {
    const char = className.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // 转换为32位整数
  }
  return hash;
}

/**
 * 获取值的哈希值
 */
function __getHashValue(value: any, propertyName: string): number {
  if (value === undefined || value === null) {
    return 0;
  }

  if (typeof value === 'boolean') {
    return value ? 1 : 0;
  }

  if (typeof value === 'number') {
    return Math.floor(value) & 0xFFFFFFFF; // 确保是32位整数
  }

  if (typeof value === 'string') {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
      const char = value.charCodeAt(i);
      hash = combine(hash, char);
      hash = hash & hash; // 转换为32位整数
    }
    return hash;
  }

  if (Array.isArray(value)) {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
      hash = combine(hash, __getHashValue(value[i], `${propertyName}[${i}]`));
      hash = hash & hash;
    }
    return hash;
  }

  if (typeof value === 'object') {
    if (value.hashCode && typeof value.hashCode === 'function') {
      return value.hashCode();
    } else {
      // 对于普通对象，递归计算哈希值
      let hash = 0;
      for (const key in value) {
        if (value.hasOwnProperty(key)) {
          hash = combine(hash, __getHashValue(key, `${propertyName}.${key}`));
          hash = hash & hash;
        }
      }
      return hash;
    }
  }

  return 0;
}

/**
 * 哈希值组合
 */
function combine(the: number, that: number): number {
  return ((the << 5) - the) + that;
}
