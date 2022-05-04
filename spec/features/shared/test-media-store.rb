shared_context :test_media_store do

  before :each do
    @media_service_setting ||= \
      (MediaServiceSetting.first || create(:media_service_setting) && MediaServiceSetting.first)

    @test_media_store = create(:media_store, :database,
                               :with_users, users: [@current_user])
  end

end
