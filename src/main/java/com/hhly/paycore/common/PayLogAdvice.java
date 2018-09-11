package com.hhly.paycore.common;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.exception.ValidException;
import com.hhly.skeleton.base.util.ObjectUtil;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * @desc Advice通知类 （用于pay的请求参数和返回结果）
 * @author xiongjingang
 * @date 2017年3月13日
 * @company 益彩网络科技公司
 * @version 1.0
 * 只有afterReturning 与 around可以获取方法的返回值
 */
@Aspect
public class PayLogAdvice {

	Logger logger = Logger.getLogger(PayLogAdvice.class);

	public static Map<String, String> typeMap = new HashMap<String, String>();

	static {
		typeMap.put("java.lang.Integer", "java.lang.Integer");
		typeMap.put("java.lang.Double", "java.lang.Double");
		typeMap.put("java.lang.Float", "java.lang.Float");
		typeMap.put("java.lang.Long", "java.lang.Long");
		typeMap.put("java.lang.Short", "java.lang.Short");
		typeMap.put("java.lang.Byte", "java.lang.Byte");
		typeMap.put("java.lang.Boolean", "java.lang.Boolean");
		typeMap.put("java.lang.Char", "java.lang.Char");
		typeMap.put("java.lang.String", "java.lang.String");
		typeMap.put("int", "int");
		typeMap.put("double", "double");
		typeMap.put("long", "long");
		typeMap.put("short", "short");
		typeMap.put("byte", "byte");
		typeMap.put("boolean", "boolean");
		typeMap.put("char", "char");
		typeMap.put("float", "float");
	}

	// 定义一个公共切入点
	// @Pointcut("execution(* com.hhly.paycore.service.impl.*.*(..))")
	// 只切到PayServiceImpl下面的方法，其它类下面的方法不做处理
	@Pointcut("execution(* com.hhly.paycore.service.impl.RechargeServiceImpl.*(..)) || execution(* com.hhly.paycore.service.impl.PayServiceImpl.*(..))")
	private void anyMethod() {
	}

	// 前置通知, 在方法执行之前执行，获取用户的请求参数
	/*	@Before("anyMethod()")
		public void doAccessCheck(JoinPoint point) throws Exception {
			String methodName = point.getSignature().getName();
			try {
				String classType = point.getTarget().getClass().getName();
				Class<?> clazz = Class.forName(classType);
				String clazzName = clazz.getName();
				String[] paramNames = getFieldsName(this.getClass(), clazzName, methodName);
				String paramsValue = writeLogInfo(paramNames, point);
				logger.info("请求方法：" + methodName + "，请求参数：" + paramsValue + "请求类：" + clazzName);
			} catch (Exception e) {
				logger.error("获取" + methodName + "请求参数异常：" + e.getMessage());
			}
		}*/

	// 返回通知, 在方法返回结果之后执行
/*	@AfterReturning(value = "anyMethod()", returning = "returnValue")
	public void doAfter(JoinPoint point, Object returnValue) {
		String methodName = point.getSignature().getName();
		logger.info("调用方法：" + methodName + " 返回值为：" + JSON.toJSON(returnValue));
	}*/

	// 后置通知, 在方法执行之后执行
	@After("anyMethod()")
	public void after(JoinPoint point) {
		// 释放资源可用
	}

	// 异常通知, 在方法抛出异常之后
	@AfterThrowing(value = "anyMethod()", throwing = "t")
	public void doAfterThrow(JoinPoint point, Exception t) {
		String methodName = point.getSignature().getName();
		if (t.getCause() instanceof ValidException) {
			logger.error("方法" + methodName + "请求参数验证不通过：" + t.getMessage());
		} else {
			logger.error("方法" + methodName + "抛出异常" + t.getMessage());
		}
	}

	/**
	 * 环绕通知, 围绕着方法执行
	 * 环绕通知需要携带 ProceedingJoinPoint 类型的参数. 
	 * 环绕通知类似于动态代理的全过程: ProceedingJoinPoint 类型的参数可以决定是否执行目标方法.
	 * 且环绕通知必须有返回值, 返回值即为目标方法的返回值
	 */
	@Around("anyMethod()")
	public Object aroundMethod(ProceedingJoinPoint pjd) {
		Object result = null;
		String methodName = pjd.getSignature().getName();
		try {
			// 执行目标方法
			result = pjd.proceed();
		} catch (Throwable e) {
			// 异常通知
			if (e instanceof ValidException) {
				return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, e.getMessage());
			} else {
				logger.error("方法" + methodName + "抛出异常", e);
				return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
			}
		}
		return result;
	}

	/**  
	* 方法说明: 获取请求参数信息
	* @auth: xiongjingang
	* @param paramNames
	* @param joinPoint
	* @time: 2017年3月14日 上午9:47:25
	* @return: String 
	*/
	private String writeLogInfo(String[] paramNames, JoinPoint joinPoint) {
		Object[] args = joinPoint.getArgs();
		StringBuilder sb = new StringBuilder();
		boolean clazzFlag = true;
		for (int k = 0; k < args.length; k++) {
			Object arg = args[k];
			sb.append(paramNames[k] + " ");
			// 获取对象类型
			String typeName = arg.getClass().getCanonicalName();
			// 基本类型判断
			if (typeMap.containsKey(typeName)) {
				sb.append("=" + arg + "; ");
				clazzFlag = false;
			}
			// map参数判断java.util.HashMap
			if ("java.util.TreeMap".equals(typeName) || "java.util.Map".equals(typeName) || "java.util.HashMap".equals(typeName)) {
				sb.append(arg.toString());
				clazzFlag = false;
			}
			if (clazzFlag) {
				sb.append(getFieldsValue(arg));
			}
		}
		return sb.toString();
	}

	/**  
	* 方法说明: 得到方法参数的名称 
	* @auth: xiongjingang
	* @param cls
	* @param clazzName
	* @param methodName
	* @throws Exception
	* @time: 2017年3月14日 上午9:47:41
	* @return: String[] 
	*/
	private String[] getFieldsName(Class<?> cls, String clazzName, String methodName) throws Exception {
		ClassPool pool = ClassPool.getDefault();
		ClassClassPath classPath = new ClassClassPath(cls);
		pool.insertClassPath(classPath);
		CtClass cc = pool.get(clazzName);
		CtMethod cm = cc.getDeclaredMethod(methodName);
		MethodInfo methodInfo = cm.getMethodInfo();
		CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
		LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
		String[] paramNames = new String[cm.getParameterTypes().length];
		int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
		for (int i = 0; i < paramNames.length; i++) {
			paramNames[i] = attr.variableName(i + pos); // paramNames即参数名
		}
		return paramNames;
	}

	/**  
	* 方法说明: 得到对象中参数的值 
	* @auth: xiongjingang
	* @param obj
	* @time: 2017年3月14日 上午9:47:57
	* @return: String 
	*/
	private String getFieldsValue(Object obj) {
		Field[] fields = obj.getClass().getDeclaredFields();
		/*String typeName = obj.getClass().getCanonicalName();
		for (String t : types) {
			if (t.equals(typeName))
				return "";
		}*/
		StringBuilder sb = new StringBuilder();
		sb.append("【");
		for (Field f : fields) {
			// 启用和禁用访问安全检查的开关。设置为true禁用安全检查，提升反射速度
			f.setAccessible(true);
			try {
				if (typeMap.containsKey(f.getType().getName())) {
					// 类型匹配上，并且不打印serialVersionUID和null对象
					if (!f.getName().equals("serialVersionUID") && !ObjectUtil.isBlank(f.get(obj))) {
						sb.append(f.getName() + " = " + f.get(obj) + "; ");
					}
				}
			} catch (Exception e) {
				logger.error("获取请求对象参数值异常" + e.getMessage());
			}
		}
		sb.append("】");
		return sb.toString();
	}

}
