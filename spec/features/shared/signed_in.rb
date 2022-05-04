
shared_context :signed_in_as_current_user do
  before :each do
    unless @current_user
      raise "current_user not set"
    end
    visit '/'
    Capybara.current_session.driver.browser.manage.add_cookie(
      name: "madek-session",
      value: MadekOpenSession.build_session_value(@current_user)
    )
    visit '/'
  end
end


shared_context :signed_in_as_a_system_admin do
  before :each do
    @system_admin ||= create(:user, :with_system_admin_role)
    @current_user = @system_admin
  end
  include_context :signed_in_as_current_user
end
