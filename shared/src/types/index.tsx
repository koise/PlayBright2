// Common types shared across Mobile and Web platforms

export interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  limit: number;
}

export interface BaseEntity {
  id: string;
  createdAt: string;
  updatedAt: string;
}

