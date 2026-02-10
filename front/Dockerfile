# --- ESTÁGIO 1: BUILD ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# Gera os arquivos estáticos na pasta dist/
RUN npm run build -- --configuration production

# --- ESTÁGIO 2: SERVIDOR (NGINX) ---
FROM nginx:alpine
# Copia os arquivos gerados pelo Angular para a pasta do Nginx
COPY --from=build /app/dist/carteira-crypto-front/browser /usr/share/nginx/html

# Copia uma configuração customizada do Nginx (Necessário para rotas do Angular não darem 404)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
