export interface JwtPayload {
  sub: number|null;
  iat: number;
  exp: number;
  roles: string[];
  profilePictureUrl: string | null;
  username: string;
  isLocalPasswordSet: boolean;
  state: string | null;
}