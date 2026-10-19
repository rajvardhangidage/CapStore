import { useEffect } from 'react'
import { Route, Routes, useLocation } from 'react-router-dom'
import Layout from './components/Layout'
import { RequireAdmin, RequireAuth } from './components/Guards'
import Home from './pages/Home'
import About from './pages/About'
import Contact from './pages/Contact'
import Products from './pages/Products'
import ProductDetail from './pages/ProductDetail'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import OrderSuccess from './pages/OrderSuccess'
import Orders from './pages/Orders'
import OrderDetail from './pages/OrderDetail'
import Login from './pages/Login'
import Register from './pages/Register'
import ForgotPassword from './pages/ForgotPassword'
import ResetPassword from './pages/ResetPassword'
import Account from './pages/Account'
import Wishlist from './pages/Wishlist'
import Quotes from './pages/Quotes'
import NotFound from './pages/NotFound'
import AdminLayout from './pages/admin/AdminLayout'
import Dashboard from './pages/admin/Dashboard'
import AdminOrders from './pages/admin/Orders'
import AdminOrderDetail from './pages/admin/OrderDetail'
import AdminProducts from './pages/admin/Products'
import ProductForm from './pages/admin/ProductForm'
import Inventory from './pages/admin/Inventory'
import Customers from './pages/admin/Customers'
import Coupons from './pages/admin/Coupons'
import AdminQuotes from './pages/admin/Quotes'
import Banners from './pages/admin/Banners'
import Reports from './pages/admin/Reports'
import StoreSettings from './pages/admin/StoreSettings'
import AdminReturns from './pages/admin/Returns'

function ScrollTop() {
  const { pathname } = useLocation()
  useEffect(() => { window.scrollTo(0, 0) }, [pathname])
  return null
}

const auth = (el) => <RequireAuth>{el}</RequireAuth>

export default function App() {
  return (
    <>
      <ScrollTop />
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="about" element={<About />} />
          <Route path="contact" element={<Contact />} />
          <Route path="products" element={<Products />} />
          <Route path="products/:id" element={<ProductDetail />} />
          <Route path="cart" element={<Cart />} />
          <Route path="login" element={<Login />} />
          <Route path="register" element={<Register />} />
          <Route path="forgot-password" element={<ForgotPassword />} />
          <Route path="reset-password" element={<ResetPassword />} />
          <Route path="wishlist" element={<Wishlist />} />
          <Route path="checkout" element={auth(<Checkout />)} />
          <Route path="order-success/:id" element={auth(<OrderSuccess />)} />
          <Route path="orders" element={auth(<Orders />)} />
          <Route path="orders/:id" element={auth(<OrderDetail />)} />
          <Route path="account" element={auth(<Account />)} />
          <Route path="quotes" element={auth(<Quotes />)} />
          <Route path="*" element={<NotFound />} />
        </Route>
        <Route path="/admin" element={<RequireAdmin><AdminLayout /></RequireAdmin>}>
          <Route index element={<Dashboard />} />
          <Route path="orders" element={<AdminOrders />} />
          <Route path="orders/:id" element={<AdminOrderDetail />} />
          <Route path="returns" element={<AdminReturns />} />
          <Route path="products" element={<AdminProducts />} />
          <Route path="products/new" element={<ProductForm />} />
          <Route path="products/:id" element={<ProductForm />} />
          <Route path="inventory" element={<Inventory />} />
          <Route path="customers" element={<Customers />} />
          <Route path="quotes" element={<AdminQuotes />} />
          <Route path="coupons" element={<Coupons />} />
          <Route path="banners" element={<Banners />} />
          <Route path="reports" element={<Reports />} />
          <Route path="settings" element={<StoreSettings />} />
        </Route>
      </Routes>
    </>
  )
}
