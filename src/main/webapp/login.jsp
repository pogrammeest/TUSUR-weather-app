<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!doctype html>
<html>
<head><meta charset="UTF-8"><title>Weather • Login</title></head>
<body>
<h2>Вход</h2>

<form action="auth" method="post">
    Логин: <input type="text" name="login" required>
    <br><br>
    Пароль: <input type="password" name="password" required>
    <br><br>
    <button type="submit">Войти</button>
</form>

<p style="color:gray">
    Тестовые пользователи: <br>
    admin / admin123 <br>
    user / user123
</p>
</body>
</html>
