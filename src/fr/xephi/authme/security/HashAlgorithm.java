package fr.xephi.authme.security;

import org.apache.commons.lang.ObjectUtils.Null;

public enum HashAlgorithm {

	MD5(fr.xephi.authme.security.crypts.MD5.class), SHA1(fr.xephi.authme.security.crypts.SHA1.class), SHA256(fr.xephi.authme.security.crypts.SHA256.class), WHIRLPOOL(fr.xephi.authme.security.crypts.WHIRLPOOL.class), XAUTH(fr.xephi.authme.security.crypts.XAUTH.class), SHA512(fr.xephi.authme.security.crypts.SHA512.class), DOUBLEMD5(fr.xephi.authme.security.crypts.DOUBLEMD5.class), PBKDF2(fr.xephi.authme.security.crypts.CryptPBKDF2.class), CUSTOM(Null.class);

	Class<?> classe;

	HashAlgorithm(Class<?> classe) {
		this.classe = classe;
	}

	public Class<?> getclass() {
		return classe;
	}

}