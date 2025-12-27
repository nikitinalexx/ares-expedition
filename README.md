# <a name="README"> Terraforming Mars: Expedition Ares Open-source

This is an open-source online implementation of the board game Terraforming Mars: Expedition Ares. **It is not affiliated
with FryxGames, Asmodee Digital or Steam in any way.**
  
**Buy The Board Game**

The board game is great and this repository highly recommends [purchasing it](https://www.amazon.com/Terraforming-Mars-Card-Game-Expedition/dp/B08QY2YYNH) for personal use.

  
  
You can play the game online at https://expedition-ares-fe.herokuapp.com/
  
This is a free heroku instance, so it may take about half a minute to warm up if it hasn't beed used for half an hour.




## Local application startup

### 🚀 Quick start

1. Start PostgreSQL via Docker:
```bash
docker pull postgres
docker run --name marsDb -p 5455:5432 -e POSTGRES_USER=root -e POSTGRES_PASSWORD=mysql -e POSTGRES_DB=myMarsDb -d postgres
```
(When using the local spring profile, database connection settings are taken from: application-local.properties. If you have a different DB address you can adjust the settings in a file)

Run backend:

Start TerraformingAresApplication.java

Use Spring profile: local

Run frontend:

```bash
cd frontend
ng serve
```
