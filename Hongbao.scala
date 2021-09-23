package main.scala


import java.text.DecimalFormat
import java.util
import java.util.Random
import java.util.Scanner


object redEnvelope {

  def nextDouble(min: Double, max: Double): Double = min + ((max - min) * new Random().nextDouble)

  def format(value: Double): String = new DecimalFormat("0.00").format(value) // 保留两位小数

  def doubleMeanMethod(nmoney: Double, number: Int): util.List[Double] = {
    var money = nmoney
    val result = new util.ArrayList[Double]
    if (money < 0 && number < 1) return null
    var amount = .0
    var sum = .0
    var remainingNumber = number
    var i = 1
    while ( {
      remainingNumber > 1
    }) {
      amount = nextDouble(0.01, 2 * (money / remainingNumber))
      sum+=(amount)
      System.out.println("第" + i + "个人领取的红包金额为：" + format(amount))
      money = money - amount
      remainingNumber -= 1
      result.add(amount)
      i += 1
    }
    result.add(money)
    System.out.println("第" + i + "个人领取的红包金额为：" + format(money))
    sum += money
    System.out.println("验证发出的红包总金额为：" + format(sum))
    result
  }



  def main(args: Array[String]): Unit = {
    val sc = new Scanner(System.in)
    var number = 0
    var money = .0
    System.out.print("请输入红包总金额：")
    money = sc.nextDouble
    System.out.print("请输入红包数量：")
    number = sc.nextInt
    doubleMeanMethod(money, number)
    System.out.println()
  }
}

