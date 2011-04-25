package com.silica.job;

import java.io.Serializable;

/**
 * Jobの実行結果が無い（void）場合に利用するための空の実行結果
 */
public abstract class Null implements Serializable {

	private static final long serialVersionUID = 1L;

	private Null() {
	}
}
