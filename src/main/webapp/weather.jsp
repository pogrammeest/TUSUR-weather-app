<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!doctype html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Прогноз погоды</title>
</head>
<body>
<h2>Прогноз погоды</h2>

<form action="weather" method="post" accept-charset="UTF-8">
    Город: <input type="text" name="city" required>
    <button type="submit">Показать</button>
</form>

<% if (request.getAttribute("error") != null) { %>
<p style="color:red"><%= request.getAttribute("error") %></p>
<% } %>

<% if (request.getAttribute("city") != null) { %>
<h3>Город: <%= request.getAttribute("city") %></h3>
<ul>
    <li>Температура: <%= request.getAttribute("temp") %> °C</li>
    <li>Состояние: <%= request.getAttribute("state") %></li>
    <li>Ветер: <%= request.getAttribute("wind") %> м/с</li>
</ul>
<% } %>

<br>
<a href="home">На главную</a>
</body>
</html>
