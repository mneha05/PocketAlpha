FROM node:24-alpine

WORKDIR /app
ENV NODE_ENV=production

COPY backend/package.json ./
COPY backend/server.mjs ./

EXPOSE 8080

CMD ["node", "server.mjs"]
