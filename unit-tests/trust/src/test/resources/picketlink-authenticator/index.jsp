<%@ page import="java.util.*,java.security.*" contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8"
%><%
Principal principal = request.getUserPrincipal();
String name=null;
if(principal != null)
   name = principal.getName();
out.write(name);
%>