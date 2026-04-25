# Stage 1: build
FROM node:25-alpine AS build
WORKDIR /app
COPY package.json .
RUN npm install

# Fail the build if any high or critical vulnerabilities are found.
# Use --audit-level=high to ignore low/moderate findings (common in transitive deps).
# To bypass temporarily: comment out this line and document why in your commit message.
RUN npm audit --audit-level=high

COPY . .
RUN npm run build

# Stage 2: serve with nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
