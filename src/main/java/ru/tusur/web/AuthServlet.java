package ru.tusur.web;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AuthServlet extends HttpServlet {

    // Простая "база" пользователей: login -> (password, role)
    private static class User {
        final String password;
        final String role; // "ADMIN" или "USER"
        User(String password, String role) { this.password = password; this.role = role; }
    }

    private static final Map<String, User> USERS = new HashMap<>();
    static {
        USERS.put("admin", new User("admin123", "ADMIN"));
        USERS.put("user",  new User("user123",  "USER"));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // /home: если залогинен — на свою страницу, иначе login.jsp
        if ("/home".equals(req.getServletPath())) {
            HttpSession session = req.getSession(false);
            if (session == null || session.getAttribute("role") == null) {
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }
            goToRoleHome(req, resp, session);
            return;
        }

        // /auth GET обычно не нужен — отправим на логин
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String login = req.getParameter("login");
        String password = req.getParameter("password");

        User user = USERS.get(login);

        // Ошибка 1: логин не существует
        if (user == null) {
            req.setAttribute("error", "Пользователь с таким логином не найден.");
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
            return;
        }

        // Ошибка 2: пароль не соответствует логину
        if (!user.password.equals(password)) {
            req.setAttribute("error", "Неверный пароль для выбранного логина.");
            req.getRequestDispatcher("/error.jsp").forward(req, resp);
            return;
        }

        // Успех: создаём сессию и ведём на страницу роли
        HttpSession session = req.getSession(true);
        session.setAttribute("login", login);
        session.setAttribute("role", user.role);

        goToRoleHome(req, resp, session);
    }

    private void goToRoleHome(HttpServletRequest req, HttpServletResponse resp, HttpSession session)
            throws ServletException, IOException {
        req.setAttribute("login", session.getAttribute("login"));
        String role = (String) session.getAttribute("role");

        if ("ADMIN".equals(role)) {
            req.getRequestDispatcher("/admin.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("/user.jsp").forward(req, resp);
        }
    }
}
