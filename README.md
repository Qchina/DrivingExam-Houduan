# DrivingExam-Houduan

驾考系统 RESTful 后端，基于 Spring Boot 3 和 Java 21 实现。

## 运行

```powershell
mvn test
mvn -DskipTests package
java -jar target\driving-exam-houduan-0.0.1-SNAPSHOT.jar
```

服务地址：

```text
http://127.0.0.1:8080/api/v1
```

## 演示账号

```json
{
  "username": "13800000000",
  "password": "123456"
}
```

登录后把返回的 token 放到请求头：

```text
Authorization: Bearer <token>
```

## 已实现接口

- `POST /auth/login`
- `POST /auth/register`
- `GET /questions`
- `GET /questions/{id}`
- `GET /questions/random`
- `GET /questions/batch`
- `GET /errors`
- `POST /errors`
- `PUT /errors/{questionId}/mastered`
- `GET /favorites`
- `POST /favorites`
- `DELETE /favorites/{questionId}`
- `GET /exams/paper`
- `POST /exams/submit`
- `GET /exams/history`
- `PUT /progress`
- `GET /stats/overview`

当前版本使用内存数据存储，内置科目一和科目四示例题库，便于前端先行联调。
