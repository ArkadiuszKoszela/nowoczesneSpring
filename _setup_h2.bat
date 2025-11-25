@echo off
echo ========================================
echo Uruchamianie aplikacji z H2 Database
echo ========================================
echo.
echo Profile aktywne: h2
echo Konsola H2: http://localhost:8081/h2-console
echo.
echo JDBC URL: jdbc:h2:mem:testdb
echo User: sa
echo Password: (puste)
echo.
echo ========================================
echo.

set SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run






















